package com.megacorp.humanresources.service.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import com.megacorp.humanresources.model.EmailMessage;

/**
 * Helper class containing utility methods for email operations.
 * Provides support for message parsing, formatting, and SMTP/IMAP session management.
 */
@Component
public class EmailServiceHelper {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceHelper.class);
    
    private static final String SMTP_SERVER = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;
    private static final String IMAP_SERVER = "imap.gmail.com";
    private static final int IMAP_PORT = 993;

    @Value("${gmail_email_address}")
    private String emailAddress;
    
    @Value("${gmail_email_app_password}")
    private String emailPassword;

    /**
     * Determines the content type based on file extension.
     *
     * @param fileName the name of the file
     * @return the MIME content type
     */
    public String determineContentType(String fileName) {
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

    /**
     * Creates and returns an authenticated SMTP session.
     *
     * @return configured SMTP Session
     */
    public Session getSmtpSession() {
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

    /**
     * Creates and returns an IMAP session configured for Gmail.
     *
     * @return configured IMAP Session
     */
    public Session getImapSession() {
        Properties props = new Properties();
        props.put("mail.imap.host", IMAP_SERVER);
        props.put("mail.imap.port", IMAP_PORT);
        props.put("mail.imap.ssl.enable", "true");
        props.put("mail.store.protocol", "imap");
        return Session.getInstance(props);
    }

    /**
     * Formats a subject string by capitalizing words separated by underscores.
     *
     * @param inputString the input string with underscores
     * @return formatted string with capitalized words
     */
    public String formatSubjectString(String inputString) {
        return Arrays.stream(inputString.split("_"))
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Retrieves an email message from the inbox by its Message-ID.
     *
     * @param messageId the Message-ID to search for
     * @param inbox the already-opened inbox folder to search in
     * @return the Message object, or null if not found
     * @throws Exception if search fails
     */
    public Message retrieveOriginalMessage(String messageId, Folder inbox) throws Exception {
        SearchTerm searchTerm = new MessageIDTerm(messageId);
        Message[] messages = inbox.search(searchTerm);
        
        if (messages.length > 0) {
            logger.debug("Found original message with ID: {}", messageId);
            return messages[0];
        } else {
            logger.warn("No message found with ID: {}", messageId);
            return null;
        }
    }

    /**
     * Formats the reply body by appending the quoted original message.
     *
     * @param newBody the new reply text from the user
     * @param originalMessage the original message being replied to
     * @return formatted reply body with quoted original content
     * @throws Exception if message parsing fails
     */
    public String formatReplyBody(String newBody, Message originalMessage) throws Exception {
        StringBuilder replyBody = new StringBuilder();
        
        // Add the new reply text
        replyBody.append(newBody);
        replyBody.append("\n\n");
        
        // Add reply header with original sender and date
        Address[] fromAddresses = originalMessage.getFrom();
        String fromAddress = "Unknown";
        if (fromAddresses != null && fromAddresses.length > 0) {
            fromAddress = fromAddresses[0].toString();
        }
        
        Date sentDate = originalMessage.getSentDate();
        String dateString = "Unknown date";
        if (sentDate != null) {
            java.time.format.DateTimeFormatter formatter = 
                java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d, yyyy 'at' h:mm a");
            LocalDateTime localDateTime = LocalDateTime.ofInstant(sentDate.toInstant(), ZoneId.systemDefault());
            dateString = localDateTime.format(formatter);
        }
        
        replyBody.append("On ").append(dateString).append(", ").append(fromAddress).append(" wrote:\n");
        
        // Extract and quote the original message body
        String originalBody = extractEmailBody(originalMessage);
        if (originalBody != null && !originalBody.trim().isEmpty()) {
            // Add '> ' prefix to each line of the original message
            String[] lines = originalBody.split("\n");
            for (String line : lines) {
                replyBody.append("> ").append(line).append("\n");
            }
        }
        
        return replyBody.toString();
    }

    /**
     * Builds a search term based on the provided filters.
     *
     * @param subjectFilter optional subject filter
     * @param fromFilter optional from filter
     * @param toFilter optional recipient filter
     * @param bodyFilter optional body text filter
     * @param messageId optional message ID for exact match
     * @param dateAfter optional date filter (yyyy-MM-dd) - emails after this date
     * @param dateBefore optional date filter (yyyy-MM-dd) - emails before this date
     * @param unreadOnly if true, only search for unread messages
     * @return SearchTerm or null if no filters applied
     */
    public SearchTerm buildSearchTerm(String subjectFilter, String fromFilter, String toFilter, 
                                      String bodyFilter, String messageId, String dateAfter, 
                                      String dateBefore, boolean unreadOnly) {
        List<SearchTerm> terms = new ArrayList<>();
        
        if (subjectFilter != null && !subjectFilter.trim().isEmpty()) {
            terms.add(new SubjectTerm(subjectFilter));
            logger.debug("Added subject filter: {}", subjectFilter);
        }
        
        if (fromFilter != null && !fromFilter.trim().isEmpty()) {
            terms.add(new FromStringTerm(fromFilter));
            logger.debug("Added from filter: {}", fromFilter);
        }
        
        if (toFilter != null && !toFilter.trim().isEmpty()) {
            terms.add(new RecipientStringTerm(Message.RecipientType.TO, toFilter));
            logger.debug("Added recipient filter: {}", toFilter);
        }
        
        if (bodyFilter != null && !bodyFilter.trim().isEmpty()) {
            terms.add(new BodyTerm(bodyFilter));
            logger.debug("Added body filter: {}", bodyFilter);
        }
        
        if (messageId != null && !messageId.trim().isEmpty()) {
            terms.add(new MessageIDTerm(messageId));
            logger.debug("Added message ID filter: {}", messageId);
        }
        
        if (dateAfter != null && !dateAfter.trim().isEmpty()) {
            try {
                java.time.LocalDate localDate = java.time.LocalDate.parse(dateAfter);
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                terms.add(new ReceivedDateTerm(ComparisonTerm.GE, date));
                logger.debug("Added date after filter: {}", dateAfter);
            } catch (Exception e) {
                logger.warn("Invalid dateAfter format: {}. Expected yyyy-MM-dd", dateAfter);
            }
        }
        
        if (dateBefore != null && !dateBefore.trim().isEmpty()) {
            try {
                java.time.LocalDate localDate = java.time.LocalDate.parse(dateBefore);
                Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                terms.add(new ReceivedDateTerm(ComparisonTerm.LE, date));
                logger.debug("Added date before filter: {}", dateBefore);
            } catch (Exception e) {
                logger.warn("Invalid dateBefore format: {}. Expected yyyy-MM-dd", dateBefore);
            }
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
    public EmailMessage convertToEmailMessage(Message message) throws Exception {
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
    public String extractEmailBody(Message message) throws Exception {
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
    public String extractTextFromMultipart(MimeMultipart multipart) throws Exception {
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
    public List<String> extractAttachmentNames(Message message) throws Exception {
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
