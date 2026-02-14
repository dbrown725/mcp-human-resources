package com.megacorp.humanresources.service;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface EmailService {
    void saveDraftEmail(String toEmail, String subject, String body, List<MultipartFile> attachmentPaths) throws Exception;
}
