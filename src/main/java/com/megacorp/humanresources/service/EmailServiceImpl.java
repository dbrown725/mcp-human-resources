package com.megacorp.humanresources.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.*;
import jakarta.activation.DataHandler;
import jakarta.mail.util.ByteArrayDataSource;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import com.megacorp.humanresources.model.EmailMessage;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    @Autowired
    private FileStorageService fileStorageService;

    private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String IMAP_SERVER = "imap.gmail.com";
    private static final int IMAP_PORT = 993;

    @Value("${gmail_email_address}")
    private String emailAddress;
    
    @Value("${gmail_email_app_password}")
    private String emailPassword;

    
    /**
     * Saves an email draft to the IMAP drafts folder with optional attachments.
     *
     * @param toEmail     recipient email address
     * @param subject     email subject (will be formatted)
     * @param body        plain text email body
     * @param attachments optional list of multipart attachments
     * @throws Exception if message creation, attachment handling, or IMAP operations fail
     */
    public void saveDraftEmail(String toEmail, String subject, String body, List<MultipartFile> attachments) throws Exception {
        MimeMessage message = new MimeMessage(getSmtpSession());
        message.setFrom(new InternetAddress(emailAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(formatSubjectString(subject));

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body, "utf-8");
        multipart.addBodyPart(textPart);

        logger.info("In saveDraft files: {}", attachments);

        if (attachments != null) {
            for (org.springframework.web.multipart.MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.setDataHandler(new DataHandler(
                            new ByteArrayDataSource(file.getInputStream(), file.getContentType())));
                    attachmentPart.setFileName(file.getOriginalFilename());
                    multipart.addBodyPart(attachmentPart);
                } else {
                    logger.warn("Warning: Empty or null file in attachments");
                }
            }
        }

        message.setContent(multipart);

        Properties props = new Properties();
        props.put("mail.imap.host", IMAP_SERVER);
        props.put("mail.imap.port", IMAP_PORT);
        props.put("mail.imap.ssl.enable", "true");
        Session imapSession = Session.getInstance(props);
        Store store = imapSession.getStore("imap");
        store.connect(IMAP_SERVER, emailAddress, emailPassword);

        Folder drafts = store.getFolder("[Gmail]/Drafts");
        if (!drafts.exists()) {
            drafts = store.getFolder("Drafts");
        }
        drafts.open(Folder.READ_WRITE);
        drafts.appendMessages(new Message[]{message});
        drafts.close(false);
        store.close();
    }

    /**
     * Saves a draft email to the user's Gmail Drafts folder with recipient, subject, body,
     * and attachments retrieved from Google Cloud Storage.
     * <p>
     * This method takes a list of Google Cloud Storage file names, retrieves them using
     * {@link FileStorageService#retrieveFile(String)}, converts them to {@link MultipartFile}
     * objects, and then calls the existing {@link #saveDraftEmail(String, String, String, List)} method.
     * </p>
     *
     * @param toEmail recipient email address
     * @param subject email subject (formatted internally)
     * @param body email body text
     * @param attachments list of Google Cloud Storage file names to attach; may be {@code null} or empty
     * @throws Exception if file retrieval, conversion, or email operations fail
     */
    // @Tool(description = "Saves an email draft to the user's Gmail Drafts folder with recipient, subject, body, " +
    //         "and attachments from Google Cloud Storage. Provide attachment file names from GCS.")
    public void saveDraftEmailFromStorage(String toEmail, String subject, String body, List<String> attachments) throws Exception {
        logger.info("Saving draft email with GCS attachments: toEmail={}, subject={}, attachmentCount={}", 
                    toEmail, subject, attachments != null ? attachments.size() : 0);
        
        List<MultipartFile> multipartFiles = new ArrayList<>();
        
        if (attachments != null && !attachments.isEmpty()) {
            for (String fileName : attachments) {
                try {
                    logger.debug("Retrieving file from GCS: {}", fileName);
                    byte[] fileContent = fileStorageService.retrieveFile(fileName);
                    
                    if (fileContent != null) {
                        // Determine content type based on file extension
                        String contentType = determineContentType(fileName);
                        
                        // Create ByteArrayMultipartFile from byte array
                        ByteArrayMultipartFile multipartFile = new ByteArrayMultipartFile(
                            "file",                    // parameter name
                            fileName,                  // original filename
                            contentType,               // content type
                            fileContent                // content
                        );
                        
                        multipartFiles.add(multipartFile);
                        logger.debug("Successfully converted file to MultipartFile: {}", fileName);
                    } else {
                        logger.warn("File not found in GCS: {}", fileName);
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving file from GCS: {}", fileName, e);
                    throw new Exception("Failed to retrieve attachment: " + fileName, e);
                }
            }
        }
        
        // Call the existing method with MultipartFile list
        saveDraftEmail(toEmail, subject, body, multipartFiles);
    }


    
    /**
     * Determines the content type based on file extension.
     *
     * @param fileName the name of the file
     * @return the MIME content type
     */
    private String determineContentType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".webp")) {
            return "image/webp";
        } else if (lowerFileName.endsWith(".heic") || lowerFileName.endsWith(".heif")) {
            return "image/heic";
        } else if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".csv")) {
            return "text/csv";
        } else if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFileName.endsWith(".doc")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        } else if (lowerFileName.endsWith(".xls")) {
            return "application/vnd.ms-excel";
        } else if (lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            return "application/octet-stream";
        }
    }

    private Session getSmtpSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_SERVER);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        return Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(emailAddress, emailPassword);
            }
        });
    }

    private String formatSubjectString(String inputString) {
        return Arrays.stream(inputString.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Custom implementation of MultipartFile for production use.
     * Wraps a byte array as a MultipartFile without using test dependencies.
     */
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content == null || content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
    
    /**
     * Reads emails from the Gmail inbox with optional filtering and limiting.
     *
     * @param maxEmails maximum number of emails to retrieve (default: 50, max: 500)
     * @param subjectFilter optional subject filter - returns emails where subject contains this string (case-insensitive)
     * @param fromFilter optional from filter - returns emails from this sender (case-insensitive)
     * @param isUnreadOnly if true, only return unread emails; if false, return all emails
     * @return list of EmailMessage objects containing email details
     * @throws Exception if IMAP connection or message parsing fails
     */
    public List<EmailMessage> readInbox(Integer maxEmails, String subjectFilter, String fromFilter, Boolean isUnreadOnly) throws Exception {
        logger.info("Reading inbox: maxEmails={}, subjectFilter={}, fromFilter={}, unreadOnly={}", 
                    maxEmails, subjectFilter, fromFilter, isUnreadOnly);
        
        // Set default and validate maxEmails
        int limit = (maxEmails != null && maxEmails > 0) ? Math.min(maxEmails, 500) : 50;
        boolean unreadOnly = (isUnreadOnly != null) ? isUnreadOnly : false;
        
        List<EmailMessage> emailMessages = new ArrayList<>();
        
        // Configure IMAP properties
        Properties props = new Properties();
        props.put("mail.imap.host", IMAP_SERVER);
        props.put("mail.imap.port", IMAP_PORT);
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.store.protocol", "imap");
        
        Session imapSession = Session.getInstance(props);
        Store store = null;
        Folder inbox = null;
        
        try {
            // Connect to Gmail IMAP
            store = imapSession.getStore("imap");
            store.connect(IMAP_SERVER, emailAddress, emailPassword);
            
            // Open inbox folder
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            logger.info("Connected to inbox. Total messages: {}", inbox.getMessageCount());
            
            // Build search criteria
            SearchTerm searchTerm = buildSearchTerm(subjectFilter, fromFilter, unreadOnly);
            
            // Search for messages
            Message[] messages;
            if (searchTerm != null) {
                messages = inbox.search(searchTerm);
                logger.info("Search returned {} messages", messages.length);
            } else {
                messages = inbox.getMessages();
            }
            
            // Limit the number of messages processed
            int messagesToProcess = Math.min(messages.length, limit);
            
            // Process messages in reverse order (newest first)
            for (int i = messages.length - 1; i >= messages.length - messagesToProcess && i >= 0; i--) {
                try {
                    Message message = messages[i];
                    EmailMessage emailMessage = convertToEmailMessage(message);
                    emailMessages.add(emailMessage);
                } catch (Exception e) {
                    logger.error("Error processing message at index {}: {}", i, e.getMessage());
                    // Continue processing other messages
                }
            }
            
            logger.info("Successfully processed {} email messages", emailMessages.size());
            
        } finally {
            // Clean up resources
            if (inbox != null && inbox.isOpen()) {
                inbox.close(false);
            }
            if (store != null && store.isConnected()) {
                store.close();
            }
        }
        
        return emailMessages;
    }
    
    /**
     * Builds a search term based on the provided filters.
     *
     * @param subjectFilter optional subject filter
     * @param fromFilter optional from filter
     * @param unreadOnly if true, only search for unread messages
     * @return SearchTerm or null if no filters applied
     */
    private SearchTerm buildSearchTerm(String subjectFilter, String fromFilter, boolean unreadOnly) {
        List<SearchTerm> terms = new ArrayList<>();
        
        if (subjectFilter != null && !subjectFilter.trim().isEmpty()) {
            terms.add(new SubjectTerm(subjectFilter));
            logger.debug("Added subject filter: {}", subjectFilter);
        }
        
        if (fromFilter != null && !fromFilter.trim().isEmpty()) {
            terms.add(new FromStringTerm(fromFilter));
            logger.debug("Added from filter: {}", fromFilter);
        }
        
        if (unreadOnly) {
            terms.add(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            logger.debug("Added unread filter");
        }
        
        if (terms.isEmpty()) {
            return null;
        } else if (terms.size() == 1) {
            return terms.get(0);
        } else {
            // Combine all terms with AND
            SearchTerm combined = terms.get(0);
            for (int i = 1; i < terms.size(); i++) {
                combined = new AndTerm(combined, terms.get(i));
            }
            return combined;
        }
    }
    
    /**
     * Converts a JavaMail Message to our EmailMessage model.
     *
     * @param message the JavaMail message
     * @return EmailMessage object
     * @throws Exception if message parsing fails
     */
    private EmailMessage convertToEmailMessage(Message message) throws Exception {
        EmailMessage emailMessage = new EmailMessage();
        
        // Message ID
        if (message instanceof MimeMessage) {
            emailMessage.setMessageId(((MimeMessage) message).getMessageID());
        }
        
        // From
        Address[] fromAddresses = message.getFrom();
        if (fromAddresses != null && fromAddresses.length > 0) {
            emailMessage.setFrom(fromAddresses[0].toString());
        }
        
        // To
        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null) {
            emailMessage.setTo(Arrays.stream(toAddresses)
                    .map(Address::toString)
                    .collect(Collectors.toList()));
        }
        
        // CC
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        if (ccAddresses != null) {
            emailMessage.setCc(Arrays.stream(ccAddresses)
                    .map(Address::toString)
                    .collect(Collectors.toList()));
        }
        
        // Subject
        emailMessage.setSubject(message.getSubject());
        
        // Body
        String body = extractEmailBody(message);
        emailMessage.setBody(body);
        
        // Sent date
        Date sentDate = message.getSentDate();
        if (sentDate != null) {
            emailMessage.setSentDate(LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault()));
        }
        
        // Received date
        Date receivedDate = message.getReceivedDate();
        if (receivedDate != null) {
            emailMessage.setReceivedDate(LocalDateTime.ofInstant(receivedDate.toInstant(), ZoneId.systemDefault()));
        }
        
        // Read status
        emailMessage.setRead(message.isSet(Flags.Flag.SEEN));
        
        // Size
        emailMessage.setSize(message.getSize());
        
        // Attachments
        List<String> attachmentNames = extractAttachmentNames(message);
        emailMessage.setAttachmentNames(attachmentNames);
        
        return emailMessage;
    }
    
    /**
     * Extracts the text body from an email message.
     *
     * @param message the email message
     * @return the email body as a string
     * @throws Exception if body extraction fails
     */
    private String extractEmailBody(Message message) throws Exception {
        Object content = message.getContent();
        
        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            return extractTextFromMultipart(multipart);
        }
        
        return "";
    }
    
    /**
     * Recursively extracts text from a multipart message.
     *
     * @param multipart the multipart content
     * @return extracted text
     * @throws Exception if extraction fails
     */
    private String extractTextFromMultipart(MimeMultipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.isMimeType("text/html")) {
                // Optionally include HTML content
                String html = bodyPart.getContent().toString();
                result.append(html);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(extractTextFromMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        
        return result.toString();
    }
    
    /**
     * Extracts attachment filenames from an email message.
     *
     * @param message the email message
     * @return list of attachment filenames
     * @throws Exception if extraction fails
     */
    private List<String> extractAttachmentNames(Message message) throws Exception {
        List<String> attachments = new ArrayList<>();
        Object content = message.getContent();
        
        if (content instanceof MimeMultipart) {
            MimeMultipart multipart = (MimeMultipart) content;
            
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    String fileName = bodyPart.getFileName();
                    if (fileName != null) {
                        attachments.add(fileName);
                    }
                }
            }
        }
        
        return attachments;
    }
    
}
