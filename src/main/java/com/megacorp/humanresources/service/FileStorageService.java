package com.megacorp.humanresources.service;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String readFile(String fileName);

    List<String> listFiles(String prefix);

    String deleteFile(String fileName);

    byte[] retrieveFile(String fileName); 

    String uploadFile(MultipartFile file);
}
