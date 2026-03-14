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
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import com.megacorp.humanresources.model.EmailMessage;
import com.megacorp.humanresources.service.helper.EmailServiceHelper;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private EmailServiceHelper emailHelper;

    private static final String IMAP_SERVER = "imap.gmail.com";

    @Value("${gmail_email_address}")
    private String emailAddress;
    
    @Value("${gmail_email_app_password}")
    private String emailPassword;

    /**
     * Saves an email draft to the IMAP drafts folder with optional attachments.
     *
     * @param toEmail     recipient email address
     * @param ccEmail     optional CC email address(es), comma-separated
     * @param subject     email subject (will be formatted)
     * @param body        plain text email body
     * @param attachments optional list of multipart attachments
     * @param storageAttachments optional list of Google Cloud Storage file names to attach
     * @param inReplyToMessageId optional Message-ID of the email this draft is replying to
     * @throws Exception if message creation, attachment handling, or IMAP operations fail
     */
    public void saveDraftEmail(String toEmail, String ccEmail, String subject, String body, List<MultipartFile> attachments, List<String> storageAttachments, String inReplyToMessageId) throws Exception {
        logger.debug("Entering saveDraftEmail with toEmail={} ccEmail={} subject={} attachmentsCount={} storageAttachmentsCount={} hasReplyTo={}",
            toEmail,
            ccEmail,
            subject,
            attachments == null ? 0 : attachments.size(),
            storageAttachments == null ? 0 : storageAttachments.size(),
            inReplyToMessageId != null && !inReplyToMessageId.trim().isEmpty());
        MimeMessage message = new MimeMessage(emailHelper.getSmtpSession());
        message.setFrom(new InternetAddress(emailAddress));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        if (ccEmail != null && !ccEmail.trim().isEmpty()) {
            message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail));
        }
        
        String emailBody = body;
        Store inboxStore = null;
        Folder inboxFolder = null;
        
        // Handle reply headers, subject, and body with quoted original message
        if (inReplyToMessageId != null && !inReplyToMessageId.trim().isEmpty()) {
            message.setHeader("In-Reply-To", inReplyToMessageId);
            message.setHeader("References", inReplyToMessageId);
            
            // Add "Re: " prefix to subject if not already present
            String formattedSubject = emailHelper.formatSubjectString(subject);
            if (!formattedSubject.toLowerCase().startsWith("re:")) {
                formattedSubject = "Re: " + formattedSubject;
            }
            message.setSubject(formattedSubject);
            
            // Open inbox folder to retrieve original message
            try {
                Session imapSession = emailHelper.getImapSession();
                inboxStore = imapSession.getStore("imap");
                inboxStore.connect(IMAP_SERVER, emailAddress, emailPassword);
                
                inboxFolder = inboxStore.getFolder("INBOX");
                inboxFolder.open(Folder.READ_ONLY);
                
                // Retrieve original message while folder is open
                Message originalMessage = emailHelper.retrieveOriginalMessage(inReplyToMessageId, inboxFolder);
                if (originalMessage != null) {
                    emailBody = emailHelper.formatReplyBody(body, originalMessage);
                    
                    // Extract and include original email's attachment names
                    List<String> originalAttachmentNames = emailHelper.extractAttachmentNames(originalMessage);
                    if (originalAttachmentNames != null && !originalAttachmentNames.isEmpty()) {
                        emailBody += "\n\n[Original email contained " + originalAttachmentNames.size() + 
                                    " attachment(s): " + String.join(", ", originalAttachmentNames) + "]";
                        logger.debug("Added original email attachment names to reply: {}", originalAttachmentNames);
                    }
                    
                    logger.debug("Successfully retrieved and quoted original message");
                } else {
                    logger.warn("Could not retrieve original message with ID: {}", inReplyToMessageId);
                }
            } catch (Exception e) {
                logger.error("Error retrieving original message for inReplyToMessageId={}", inReplyToMessageId, e);
                // Continue with reply without quoted content
            } finally {
                // Close inbox folder after we're done with the message
                try {
                    if (inboxFolder != null && inboxFolder.isOpen()) {
                        inboxFolder.close(false);
                    }
                } catch (Exception e) {
                    logger.warn("Error closing inbox folder", e);
                }
                try {
                    if (inboxStore != null && inboxStore.isConnected()) {
                        inboxStore.close();
                    }
                } catch (Exception e) {
                    logger.warn("Error closing inbox store", e);
                }
            }
            
            logger.debug("Creating draft as reply to message ID: {}", inReplyToMessageId);
        } else {
            message.setSubject(emailHelper.formatSubjectString(subject));
        }

        MimeMultipart multipart = new MimeMultipart();
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(emailBody, "utf-8");
        multipart.addBodyPart(textPart);

        logger.debug("Preparing draft attachments with multipartCount={} storageCount={}",
            attachments == null ? 0 : attachments.size(),
            storageAttachments == null ? 0 : storageAttachments.size());

        // Add MultipartFile attachments
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
        
        // Add Google Cloud Storage attachments
        if (storageAttachments != null && !storageAttachments.isEmpty()) {
            for (String fileName : storageAttachments) {
                try {
                    logger.debug("Retrieving file from GCS: {}", fileName);
                    byte[] fileContent = fileStorageService.retrieveFile(fileName);
                    
                    if (fileContent != null) {
                        String contentType = emailHelper.determineContentType(fileName);
                        
                        MimeBodyPart attachmentPart = new MimeBodyPart();
                        attachmentPart.setDataHandler(new DataHandler(
                                new ByteArrayDataSource(fileContent, contentType)));
                        attachmentPart.setFileName(fileName);
                        multipart.addBodyPart(attachmentPart);
                        
                        logger.debug("Successfully attached file from GCS: {}", fileName);
                    } else {
                        logger.warn("File not found in GCS: {}", fileName);
                    }
                } catch (Exception e) {
                    logger.error("Error retrieving file from GCS: {}", fileName, e);
                    throw new Exception("Failed to retrieve attachment from storage: " + fileName, e);
                }
            }
        }

        message.setContent(multipart);

        Session imapSession = emailHelper.getImapSession();
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
        logger.info("Email draft saved successfully for toEmail={} subject={}", toEmail, subject);
    }
    
    /**
     * Reads emails from the Gmail inbox with optional filtering and limiting.
     *
     * @param maxEmails maximum number of emails to retrieve (default: 50, max: 500)
     * @param subjectFilter optional subject filter - returns emails where subject contains this string (case-insensitive)
     * @param fromFilter optional from filter - returns emails from this sender (case-insensitive)
     * @param toFilter optional recipient filter - returns emails sent to this recipient (case-insensitive)
     * @param bodyFilter optional body filter - returns emails where body contains this string (case-insensitive)
     * @param messageId optional message ID - returns specific email by message ID
     * @param dateAfter optional date filter - returns emails received after this date (format: yyyy-MM-dd)
     * @param dateBefore optional date filter - returns emails received before this date (format: yyyy-MM-dd)
     * @param isUnreadOnly if true, only return unread emails; if false, return all emails
     * @return list of EmailMessage objects containing email details
     * @throws Exception if IMAP connection or message parsing fails
     */
    @Tool(name = "readInbox", description = "Reads emails from the Gmail inbox with optional filtering by " +
            "subject, sender, recipient, body content, date range, and read/unread status. Returns up to the specified " +
            "maximum number of emails (default 50, max 500).")
    public List<EmailMessage> readInbox(Integer maxEmails, String subjectFilter, String fromFilter, String toFilter, 
                                        String bodyFilter, String messageId, String dateAfter, String dateBefore, 
                                        Boolean isUnreadOnly) throws Exception {
        return readFromFolder("INBOX", maxEmails, subjectFilter, fromFilter, toFilter, bodyFilter, messageId, dateAfter, dateBefore, isUnreadOnly);
    }

    /**
     * Reads emails from a specified Gmail folder with optional filtering and limiting.
     * Common folder names: "INBOX", "[Gmail]/Drafts", "[Gmail]/Sent Mail", "[Gmail]/Trash", "[Gmail]/All Mail".
     *
     * @param folderName    the IMAP folder name to read from
     * @param maxEmails     maximum number of emails to retrieve (default: 50, max: 500)
     * @param subjectFilter optional subject filter (case-insensitive contains)
     * @param fromFilter    optional sender filter (case-insensitive contains)
     * @param toFilter      optional recipient filter (case-insensitive contains)
     * @param bodyFilter    optional body filter (case-insensitive contains)
     * @param messageId     optional exact message ID match
     * @param dateAfter     optional date filter (format: yyyy-MM-dd)
     * @param dateBefore    optional date filter (format: yyyy-MM-dd)
     * @param isUnreadOnly  if true, only return unread emails
     * @return list of EmailMessage objects
     * @throws Exception if IMAP connection or message parsing fails
     */
    public List<EmailMessage> readFolder(String folderName, Integer maxEmails, String subjectFilter, String fromFilter, 
                                         String toFilter, String bodyFilter, String messageId, String dateAfter, 
                                         String dateBefore, Boolean isUnreadOnly) throws Exception {
        if (folderName == null || folderName.trim().isEmpty()) {
            folderName = "INBOX";
        }
        return readFromFolder(folderName, maxEmails, subjectFilter, fromFilter, toFilter, bodyFilter, messageId, dateAfter, dateBefore, isUnreadOnly);
    }

    /**
     * Core implementation for reading emails from any Gmail IMAP folder.
     */
    private List<EmailMessage> readFromFolder(String folderName, Integer maxEmails, String subjectFilter, 
                                              String fromFilter, String toFilter, String bodyFilter, 
                                              String messageId, String dateAfter, String dateBefore, 
                                              Boolean isUnreadOnly) throws Exception {
        logger.debug("Entering readFromFolder with folder={}, maxEmails={}, subjectFilter={}, fromFilter={}, toFilter={}, bodyFilter={}, messageId={}, dateAfter={}, dateBefore={}, unreadOnly={}", 
                    folderName, maxEmails, subjectFilter, fromFilter, toFilter, bodyFilter, messageId, dateAfter, dateBefore, isUnreadOnly);
        
        int limit = (maxEmails != null && maxEmails > 0) ? Math.min(maxEmails, 500) : 50;
        boolean unreadOnly = (isUnreadOnly != null) ? isUnreadOnly : false;
        
        List<EmailMessage> emailMessages = new ArrayList<>();
        
        Session imapSession = emailHelper.getImapSession();
        Store store = null;
        Folder folder = null;
        
        try {
            store = imapSession.getStore("imap");
            store.connect(IMAP_SERVER, emailAddress, emailPassword);
            
            folder = store.getFolder(folderName);
            if (!folder.exists()) {
                logger.warn("Folder '{}' does not exist", folderName);
                return emailMessages;
            }
            folder.open(Folder.READ_ONLY);
            
            logger.info("Connected to folder '{}'. Total messages: {}", folderName, folder.getMessageCount());
            
            SearchTerm searchTerm = emailHelper.buildSearchTerm(subjectFilter, fromFilter, toFilter, bodyFilter, messageId, dateAfter, dateBefore, unreadOnly);
            
            Message[] messages;
            if (searchTerm != null) {
                messages = folder.search(searchTerm);
                logger.info("Search returned {} messages", messages.length);
            } else {
                messages = folder.getMessages();
            }
            
            int messagesToProcess = Math.min(messages.length, limit);
            
            for (int i = messages.length - 1; i >= messages.length - messagesToProcess && i >= 0; i--) {
                try {
                    Message message = messages[i];
                    EmailMessage emailMessage = emailHelper.convertToEmailMessage(message);
                    emailMessages.add(emailMessage);
                } catch (Exception e) {
                    logger.error("Error processing message at index {}: {}", i, e.getMessage());
                }
            }
            
            logger.info("Successfully processed {} email messages from folder '{}'", emailMessages.size(), folderName);
            
        } catch (Exception e) {
            logger.error("Error reading folder '{}'", folderName, e);
            throw e;
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
            } catch (Exception e) {
                logger.warn("Error closing folder", e);
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing store", e);
            }
        }
        
        return emailMessages;
    }
    
    /**
     * Marks an email as read by setting the SEEN flag.
     *
     * @param messageId the Message-ID of the email to mark as read
     * @throws Exception if IMAP connection fails or message is not found
     */
    @Tool(name = "markEmailAsRead", description = "Marks a specific email as read by setting the SEEN flag. " +
            "Requires the Message-ID of the email to mark as read.")
    public void markEmailAsRead(String messageId) throws Exception {
        logger.debug("Entering markEmailAsRead with messageId={}", messageId);
        
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be null or empty");
        }
        
        Session imapSession = emailHelper.getImapSession();
        Store store = null;
        Folder inbox = null;
        
        try {
            // Connect to Gmail IMAP
            store = imapSession.getStore("imap");
            store.connect(IMAP_SERVER, emailAddress, emailPassword);
            
            // Open inbox folder in READ_WRITE mode to modify flags
            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);
            
            logger.debug("Connected to inbox in READ_WRITE mode");
            
            // Search for the message by Message-ID
            SearchTerm searchTerm = new MessageIDTerm(messageId);
            Message[] messages = inbox.search(searchTerm);
            
            if (messages.length == 0) {
                throw new Exception("Email not found with Message-ID: " + messageId);
            }
            
            // Mark the message as read (set SEEN flag)
            Message message = messages[0];
            message.setFlag(Flags.Flag.SEEN, true);
            
            logger.info("Successfully marked email as read: {}", messageId);
            
        } catch (Exception e) {
            logger.error("Error marking email as read for messageId={}", messageId, e);
            throw e;
        } finally {
            // Clean up resources
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(true); // true to expunge deleted messages and save changes
                }
            } catch (Exception e) {
                logger.warn("Error closing inbox", e);
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing store", e);
            }
        }
    }

    /**
     * Deletes all draft emails whose subject contains the given text.
     * Used for rollback during onboarding workflow failures.
     */
    @Override
    public int deleteDraftsBySubjectContaining(String subjectContains) throws Exception {
        logger.debug("Entering deleteDraftsBySubjectContaining with subjectContains={}", subjectContains);
        Store store = null;
        Folder drafts = null;
        int deletedCount = 0;

        try {
            Session imapSession = emailHelper.getImapSession();
            store = imapSession.getStore("imap");
            store.connect(IMAP_SERVER, emailAddress, emailPassword);

            drafts = store.getFolder("[Gmail]/Drafts");
            if (!drafts.exists()) {
                drafts = store.getFolder("Drafts");
            }
            drafts.open(Folder.READ_WRITE);

            Message[] messages = drafts.getMessages();
            for (Message msg : messages) {
                String subject = msg.getSubject();
                if (subject != null && subject.contains(subjectContains)) {
                    msg.setFlag(Flags.Flag.DELETED, true);
                    deletedCount++;
                    logger.debug("Marked draft for deletion: {}", subject);
                }
            }

            if (deletedCount > 0) {
                drafts.expunge();
            }

            logger.info("Deleted {} draft emails matching '{}'", deletedCount, subjectContains);
            return deletedCount;

        } catch (Exception e) {
            logger.error("Error deleting drafts matching '{}': {}", subjectContains, e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (drafts != null && drafts.isOpen()) {
                    drafts.close(false);
                }
            } catch (Exception e) {
                logger.warn("Error closing drafts folder", e);
            }
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (Exception e) {
                logger.warn("Error closing store", e);
            }
        }
    }
    
}
