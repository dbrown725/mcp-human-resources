package com.megacorp.humanresources.service;

import org.springframework.web.multipart.MultipartFile;
import com.megacorp.humanresources.model.EmailMessage;
import java.util.List;

public interface EmailService {
    void saveDraftEmail(String toEmail, String subject, String body, List<MultipartFile> attachmentPaths) throws Exception;
    
    List<EmailMessage> readInbox(Integer maxEmails, String subjectFilter, String fromFilter, Boolean isUnreadOnly) throws Exception;
}
