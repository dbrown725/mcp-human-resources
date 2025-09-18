package com.megacorp.humanresources.service;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String readFile(String fileName);

    List<String> listFiles(String prefix);

    String deleteFile(String fileName);

    byte[] retrieveFile(String fileName);

    String uploadFile(MultipartFile file);

    String uploadFile(byte[] fileContent, String fileName);

    List<String> listFileUrlsInFolder(String folderName);

    Resource getResourceFromGcsUrl(String gcsUrl);
}
