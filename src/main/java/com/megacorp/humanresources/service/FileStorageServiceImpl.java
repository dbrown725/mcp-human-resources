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
/**
 * Implementation of the {@link FileStorageService} interface for managing files in Google Cloud Storage (GCS).
 * <p>
 * This service provides methods to upload, retrieve, read, delete, and list files in a specified GCS bucket.
 * It uses the Google Cloud Storage client library to interact with GCS and supports operations such as:
 * <ul>
 *     <li>Uploading files to a GCS bucket</li>
 *     <li>Downloading files from a GCS bucket</li>
 *     <li>Reading the contents of a file as a string</li>
 *     <li>Deleting files from a GCS bucket</li>
 *     <li>Listing files in a GCS bucket with a given prefix</li>
 * </ul>
 * <p>
 * The bucket name and project ID are injected from application properties.
 * <p>
 * Logging is provided for all operations to facilitate debugging and monitoring.
 * <p>
 * Methods are annotated with {@code @Tool} to enable integration with external tools or frameworks.
 *
 * @author davidbrown
 */
@Service
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Value("${google.cloud.storage.bucket.name}")
    private String bucketName;

    @Value("${spring.ai.vertex.ai.gemini.project-id}")
    private String projectId;


    /**
     * Uploads a file to Google Cloud Storage.
     *
     * @param file the file to upload
     * @return a message indicating the result of the upload operation
     */
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
            logger.error(errorMsg);
            return errorMsg;
        }
        
        logger.debug("File {} as {}", fileName, fileName);
        logger.info("Exiting uploadFile method");
        return "File " + fileName + " uploaded as " + fileName;
    }

    
    /**
     * Downloads a file from Google Cloud Storage and returns its contents as a byte array.
     *
     * @param fileName the name of the file to download
     * @return the contents of the file as a byte array, or null if the file does not exist
     */
    @Tool(name = "storage_retrieve_file", description = "Downloads a file from Google Cloud Storage")
    @Override
    public byte[] retrieveFile(String fileName) {
        logger.info("Entering retrieveFile method");

        // Create a new GCS client and get the blob object from the blob ID
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId = BlobId.of(bucketName, fileName);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            logger.error("File {} does not exist in bucket {}", fileName, bucketName);
            return null;
        }

        logger.debug("File {} downloaded from bucket {}, contents will be returned", fileName, bucketName);
        logger.info("Exiting retrieveFile method");
        return blob.getContent();
    }

    /**
     * Reads the contents of a file from Google Cloud Storage and returns it as a String.
     *
     * @param fileName the name of the file to read
     * @return the contents of the file as a String, or an error message if not found or on exception
     */
    @Tool(name = "storage_read_file", description = "Returns the contents of a file from Google Cloud Storage")
    @Override
    public String readFile(String fileName)  {
        logger.info("Entering readFile method");
        // Create a new GCS client and get the blob object from the blob ID
        String contents = "";
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            Blob blob = storage.get(blobId);
            if (blob == null) {
                logger.error("File {} does not exist in bucket {}", fileName, bucketName);
                return "File not found";
            }

            // read the contents of the file and print it
            contents = new String(blob.getContent());
            logger.debug("Contents of file {}: {}", fileName, contents);
        } catch (Exception e) {
            contents = "Exception occurred: " + e.getMessage();
        }
        logger.debug("Contents of file {}: {}", fileName, contents);
        logger.info("Exiting readFile method");
        return contents;
    }

    /**
     * Deletes a file from Google Cloud Storage.
     *
     * @param fileName the name of the file to delete
     * @return a message indicating the result of the delete operation
     */
    @Tool(name = "storage_delete_file", description = "Deletes a file from Google Cloud Storage")
    @Override
    public String deleteFile(String fileName) {
        logger.info("Entering deleteFile method");
        // Create a new GCS client and get the blob object from the blob ID
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            BlobId blobId = BlobId.of(bucketName, fileName);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                logger.error("File {} does not exist in bucket {}", fileName, bucketName);
                return "File not found";
            }

            // delete the file and print the status
            blob.delete();
        } catch (Exception e) {
            logger.error("Exception occurred while deleting file: {}", e.getMessage());
            return "Exception occurred: " + e.getMessage();
        }
        
        logger.info("Exiting deleteFile method");
        return "File " + fileName + " deleted from bucket " + bucketName;
    }

    /**
     * Lists all files in a Google Cloud Storage bucket with a given prefix.
     *
     * @param prefix the prefix to filter files in the bucket
     * @return a list of file names matching the prefix
     */
    @Tool(name = "storage_list_files", description = "Lists all files in a Google Cloud Storage bucket with a given prefix")
    @Override
    public List<String> listFiles(String prefix) {
        logger.info("Entering listFiles method");

        List<String> fileNames = new ArrayList<>();
        try {
            // Create a new GCS client and get the blob object from the blob ID
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();

            logger.debug("Files in bucket {}:", bucketName);
            // List all the blobs in the bucket with the specified prefix
            for (Blob blob : storage
                .list(bucketName, BlobListOption.currentDirectory(), BlobListOption.prefix(prefix))
                .iterateAll()) {
                logger.debug(blob.getName());
                fileNames.add(blob.getName());
            }
        } catch (Exception e) {
            logger.error("Exception occurred while listing files: {}", e.getMessage());
            throw e;
        }
        logger.info("Exiting listFiles method");
        return fileNames;
    }
}
