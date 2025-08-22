package com.megacorp.humanresources.service;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import lombok.extern.slf4j.Slf4j;

import com.google.cloud.storage.Storage.BlobListOption;

// Based on https://github.com/sohamkamani/java-gcp-examples/blob/main/src/main/java/com/sohamkamani/storage/App.java
// https://www.youtube.com/watch?v=FXiV4WPQveY
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${google.cloud.storage.bucket.name}")
    private String bucketName;

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;

    @Override
    public String uploadFile(MultipartFile file){
        logger.info("Entering uploadFile method");
        // Create a new GCS client
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

        String fileName;
        byte[] fileContent;
        BlobId blobId;
        BlobInfo blobInfo;
        try {
            fileName = file.getOriginalFilename();
            fileContent = file.getBytes();
            blobId = BlobId.of(bucketName, fileName);
            blobInfo = BlobInfo.newBuilder(blobId).setContentType("application/octet-stream").build();
            storage.create(blobInfo, fileContent);
        } catch (Exception e) {
            String errorMsg = "Failed to upload file: " + e.getMessage();
            System.out.println(errorMsg);
            return errorMsg;
        }
        
        logger.debug("File {} as {}", fileName, fileName);
        logger.info("Exiting uploadFile method");
        return "File " + fileName + " uploaded as " + fileName;
    }

    @Tool(name = "storage_retrieve_file", description = "Downloads a file from Google Cloud Storage")
    @Override
    public byte[] retrieveFile(String fileName) {

        // Create a new GCS client and get the blob object from the blob ID
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            System.out.println("File " + fileName + " does not exist in bucket " + bucketName);
            return null;
        }

        // download the file and print the status
        //blob.downloadTo(Paths.get(filePath));
        System.out.println("File " + fileName + " returned  downloaded from bucket, contents will be returned");
        return blob.getContent();
    }

    @Tool(name = "storage_read_file", description = "Returns the contents of a file from Google Cloud Storage")
    public String readFile(String fileName)  {
        // Create a new GCS client and get the blob object from the blob ID
        String contents = "";
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            Blob blob = storage.get(blobId);
            if (blob == null) {
                System.out.println("File " + fileName + " does not exist in bucket " + bucketName);
                return "File not found";
            }

            // read the contents of the file and print it
            contents = new String(blob.getContent());
            System.out.println("Contents of file " + fileName + ": " + contents);
        } catch (Exception e) {
            contents = "Exception occurred: " + e.getMessage();
        }
        return contents;
    }

    @Tool(name = "storage_delete_file", description = "Deletes a file from Google Cloud Storage")
    public String deleteFile(String fileName) {
        // Create a new GCS client and get the blob object from the blob ID
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                System.out.println("File " + fileName + " does not exist in bucket " + bucketName);
                return "File not found";
            }

            // delete the file and print the status
            blob.delete();
        } catch (Exception e) {
            System.out.println("Exception occurred while deleting file: " + e.getMessage());
            return "Exception occurred: " + e.getMessage();
        }
        System.out.println("File " + fileName + " deleted from bucket " + bucketName);
        return "File " + fileName + " deleted from bucket " + bucketName;
    }

    @Tool(name = "storage_list_files", description = "Lists all files in a Google Cloud Storage bucket with a given prefix")
    public List<String> listFiles(String prefix) {

        List<String> fileNames = new ArrayList<>();
        try {
            // Create a new GCS client and get the blob object from the blob ID
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

            System.out.println("Files in bucket " + bucketName + ":");
            // list all the blobs in the bucket
            for (Blob blob : storage
                // .list(bucketName, BlobListOption.currentDirectory(), BlobListOption.prefix("user_data/"))
                .list(bucketName, BlobListOption.currentDirectory(), BlobListOption.prefix(prefix))
                .iterateAll()) {
            System.out.println(blob.getName());
            fileNames.add(blob.getName());
            }
        } catch (Exception e) {
            System.out.println("Exception occurred while listing files: " + e.getMessage());
            throw e;
        }
        return fileNames;
    }
}
