package com.megacorp.humanresources.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;

import com.megacorp.humanresources.service.EmailService;
import com.megacorp.humanresources.model.EmailMessage;

@RestController
public class EmailController {

    @Autowired
	private EmailService emailService;


    @PostMapping("/save-draft-email")
    public ResponseEntity<String> saveDraft(
            @RequestParam("toEmail") String toEmail,
            @RequestParam("subject") String subject,
            @RequestParam("body") String body,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestParam(value = "storageAttachments", required = false) List<String> storageAttachments,
            @RequestParam(value = "inReplyToMessageId", required = false) String inReplyToMessageId) {
        try {
            emailService.saveDraftEmail(toEmail, subject, body, attachments, storageAttachments, inReplyToMessageId);
            return ResponseEntity.ok("Draft saved.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to save draft.");
        }
    }
    
    /**
     * Reads emails from the Gmail inbox with optional filtering.
     *
     * @param maxEmails maximum number of emails to retrieve (optional, default: 50, max: 500)
     * @param subjectFilter filter by subject containing this text (optional, case-insensitive)
     * @param fromFilter filter by sender email containing this text (optional, case-insensitive)
     * @param toFilter filter by recipient email containing this text (optional, case-insensitive)
     * @param bodyFilter filter by body text containing this string (optional, case-insensitive)
     * @param messageId filter by specific message ID (optional, exact match)
     * @param dateAfter filter emails received after this date (optional, format: yyyy-MM-dd)
     * @param dateBefore filter emails received before this date (optional, format: yyyy-MM-dd)
     * @param unreadOnly if true, only return unread emails (optional, default: false)
     * @return list of email messages with details
     */
    @GetMapping("/read-inbox")
    public ResponseEntity<?> readInbox(
            @RequestParam(value = "maxEmails", required = false) Integer maxEmails,
            @RequestParam(value = "subjectFilter", required = false) String subjectFilter,
            @RequestParam(value = "fromFilter", required = false) String fromFilter,
            @RequestParam(value = "toFilter", required = false) String toFilter,
            @RequestParam(value = "bodyFilter", required = false) String bodyFilter,
            @RequestParam(value = "messageId", required = false) String messageId,
            @RequestParam(value = "dateAfter", required = false) String dateAfter,
            @RequestParam(value = "dateBefore", required = false) String dateBefore,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly) {
        try {
            List<EmailMessage> emails = emailService.readInbox(maxEmails, subjectFilter, fromFilter, toFilter, 
                                                              bodyFilter, messageId, dateAfter, dateBefore, unreadOnly);
            return ResponseEntity.ok(emails);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read inbox: " + ex.getMessage());
        }
    }
    
    /**
     * Marks an email as read by setting the SEEN flag.
     *
     * @param messageId the Message-ID of the email to mark as read (required)
     * @return success or error message
     */
    @PostMapping("/mark-email-read")
    public ResponseEntity<String> markEmailAsRead(
            @RequestParam("messageId") String messageId) {
        try {
            emailService.markEmailAsRead(messageId);
            return ResponseEntity.ok("Email marked as read successfully.");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid request: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to mark email as read: " + ex.getMessage());
        }
    }

}
