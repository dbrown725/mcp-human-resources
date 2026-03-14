package com.megacorp.humanresources.service;

import org.springframework.web.multipart.MultipartFile;
import com.megacorp.humanresources.model.EmailMessage;
import java.util.List;

public interface EmailService {
    void saveDraftEmail(String toEmail, String ccEmail, String subject, String body, List<MultipartFile> attachments, List<String> storageAttachments, String inReplyToMessageId) throws Exception;
    
    List<EmailMessage> readInbox(Integer maxEmails, String subjectFilter, String fromFilter, String toFilter, 
                                 String bodyFilter, String messageId, String dateAfter, String dateBefore, 
                                 Boolean isUnreadOnly) throws Exception;

    List<EmailMessage> readFolder(String folderName, Integer maxEmails, String subjectFilter, String fromFilter, 
                                  String toFilter, String bodyFilter, String messageId, String dateAfter, 
                                  String dateBefore, Boolean isUnreadOnly) throws Exception;
    
    void markEmailAsRead(String messageId) throws Exception;

    /**
     * Deletes all draft emails whose subject contains the given prefix.
     * Used for rollback during onboarding workflow failures.
     *
     * @param subjectContains the text to match in draft email subjects
     * @return the number of drafts deleted
     * @throws Exception if IMAP operations fail
     */
    int deleteDraftsBySubjectContaining(String subjectContains) throws Exception;
}
