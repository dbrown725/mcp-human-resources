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
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments) {
        try {
            emailService.saveDraftEmail(toEmail, subject, body, attachments);
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
     * @param unreadOnly if true, only return unread emails (optional, default: false)
     * @return list of email messages with details
     */
    @GetMapping("/read-inbox")
    public ResponseEntity<?> readInbox(
            @RequestParam(value = "maxEmails", required = false) Integer maxEmails,
            @RequestParam(value = "subjectFilter", required = false) String subjectFilter,
            @RequestParam(value = "fromFilter", required = false) String fromFilter,
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly) {
        try {
            List<EmailMessage> emails = emailService.readInbox(maxEmails, subjectFilter, fromFilter, unreadOnly);
            return ResponseEntity.ok(emails);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to read inbox: " + ex.getMessage());
        }
    }

}
