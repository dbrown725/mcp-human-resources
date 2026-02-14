package com.megacorp.humanresources.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import java.util.List;

import com.megacorp.humanresources.service.EmailService;

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
    

}
