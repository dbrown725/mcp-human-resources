package com.megacorp.humanresources.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.List;

import com.megacorp.humanresources.service.FileStorageService;

@RestController
public class FileStorageController {

    @Autowired
	private FileStorageService fileStorageService;

    @GetMapping("/read-file")
	public String readFile(@RequestParam(value = "fileName") String fileName) {
		return fileStorageService.readFile(fileName);
    }

    @GetMapping("/list-files")
	public List<String> listFiles(@RequestParam(value = "prefix") String prefix) {
		return fileStorageService.listFiles(prefix);
    }

    @GetMapping("/delete-file")
	public String deleteFile(@RequestParam(value = "fileName") String fileName) {
		return fileStorageService.deleteFile(fileName);
    }

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return "Please select a file to upload.";
        }
        return fileStorageService.uploadFile(file);
    }

    @GetMapping("/download-file/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String fileName) {
        String fileNameResolved = fileName.replaceAll("SLASH", "/");
        byte[] fileContent = fileStorageService.retrieveFile(fileNameResolved);

        if (fileContent == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        // Set content type based on file extension
        String lowerFileName = fileNameResolved.toLowerCase();
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
        } 
        else {
            throw new IllegalArgumentException(
                "Unsupported file extension. Valid image extensions are: .jpg, .jpeg, .png, .gif, .webp, .heic, .heif"
            );
        }
        headers.setContentDispositionFormData("attachment", fileNameResolved);

        return new ResponseEntity<>(fileContent, headers, HttpStatus.OK);
    }
}
