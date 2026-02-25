package com.megacorp.humanresources.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

import com.megacorp.humanresources.service.FileStorageService;

@RestController
public class FileStorageController {

    private static final Logger log = LoggerFactory.getLogger(FileStorageController.class);

    @Autowired
	private FileStorageService fileStorageService;

    @GetMapping("/read-file")
	public String readFile(@RequestParam(value = "fileName") String fileName) {
        log.debug("Entering readFile with fileName={}", fileName);
        String content = fileStorageService.readFile(fileName);
        log.info("File read request completed for fileName={}", fileName);
        return content;
    }

    @GetMapping("/list-files")
	public List<String> listFiles(@RequestParam(value = "prefix") String prefix) {
        log.debug("Entering listFiles with prefix={}", prefix);
        List<String> files = fileStorageService.listFiles(prefix);
        log.info("Listed {} files for prefix={}", files.size(), prefix);
        return files;
    }

    @GetMapping("/delete-file")
	public String deleteFile(@RequestParam(value = "fileName") String fileName) {
        log.debug("Entering deleteFile with fileName={}", fileName);
        String result = fileStorageService.deleteFile(fileName);
        log.info("Delete file request completed for fileName={}", fileName);
        return result;
    }

    @PostMapping("/upload-multiple")
    public List<String> uploadFiles(@RequestParam("files") List<MultipartFile> files) {
        log.debug("Entering uploadFiles with filesCount={}", files == null ? 0 : files.size());
        if (files == null || files.isEmpty()) {
            log.warn("Upload multiple requested with no files provided");
            return List.of("Please select files to upload.");
        }
        List<String> uploaded = files.stream()
                .filter(file -> !file.isEmpty())
                .map(fileStorageService::uploadFile)
                .toList();
        log.info("Uploaded {} files successfully", uploaded.size());
        return uploaded;
    }
    
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        log.debug("Entering uploadFile with originalFilename={}", file.getOriginalFilename());
        if (file.isEmpty()) {
            log.warn("Upload requested with empty file payload");
            return "Please select a file to upload.";
        }
        String uploadedFileName = fileStorageService.uploadFile(file);
        log.info("File uploaded successfully as {}", uploadedFileName);
        return uploadedFileName;
    }

    @GetMapping("/download-file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam("fileName") String fileName) {
        log.debug("Entering downloadFile with fileName={}", fileName);
        byte[] fileContent = fileStorageService.retrieveFile(fileName);

        if (fileContent == null) {
            log.warn("Download requested for missing fileName={}", fileName);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        // Set content type based on file extension
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            headers.setContentType(MediaType.IMAGE_JPEG);
        } else if (lowerFileName.endsWith(".png")) {
            headers.setContentType(MediaType.IMAGE_PNG);
        } else if (lowerFileName.endsWith(".gif")) {
            headers.setContentType(MediaType.IMAGE_GIF);
        } else if (lowerFileName.endsWith(".webp")) {
            headers.setContentType(MediaType.parseMediaType("image/webp"));
        } else if (lowerFileName.endsWith(".heic")) {
            headers.setContentType(MediaType.parseMediaType("image/heic"));
        } else if (lowerFileName.endsWith(".heif")) {
            headers.setContentType(MediaType.parseMediaType("image/heif"));
        } else if (lowerFileName.endsWith(".csv")) {
            headers.setContentType(MediaType.parseMediaType("text/csv"));
        } else if (lowerFileName.endsWith(".txt")) {
            headers.setContentType(MediaType.parseMediaType("text/plain"));
        }
        else {
            log.error("Unsupported file extension for fileName={}", fileName);
            throw new IllegalArgumentException(
                "Unsupported file extension. Valid image extensions are: .jpg, .jpeg, .png, .gif, .webp, .heic, .heif, .csv, .txt"
            );
        }
        headers.setContentDispositionFormData("attachment", fileName);
        log.info("File downloaded successfully for fileName={}", fileName);

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}
