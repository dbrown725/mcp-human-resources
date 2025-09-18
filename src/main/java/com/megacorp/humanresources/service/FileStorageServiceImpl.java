package com.megacorp.humanresources.service;

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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

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

    /**
     * Uploads a file to Google Cloud Storage.
     *
     * @param file the file to upload
     * @return a message indicating the result of the upload operation
     */
    public String uploadFile(MultipartFile file){
        logger.info("Entering uploadFile MultipartFile file method");

        String fileName;
        byte[] fileContent;
        try {
            fileName = file.getOriginalFilename();
            fileContent = file.getBytes();
            storeFile(fileName, fileContent);
        } catch (Exception e) {
            String errorMsg = "Failed to upload file: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }
        
        logger.debug("File {} as {}", fileName, fileName);
        logger.info("Exiting uploadFile MultipartFile file method");
        return "File " + fileName + " uploaded as " + fileName;
    }

    /**
     * Uploads a file to Google Cloud Storage using byte array and file name.
     *
     * @param fileContent the content of the file as a byte array
     * @param fileName the name of the file to upload
     * @return a message indicating the result of the upload operation
     */
    public String uploadFile(byte[] fileContent, String fileName) {
        logger.info("Entering uploadFile(byte[], String) method");

        try {
            storeFile(fileName, fileContent);
        } catch (Exception e) {
            String errorMsg = "Failed to upload file: " + e.getMessage();
            logger.error(errorMsg);
            return errorMsg;
        }

        logger.debug("File {} uploaded as {}", fileName, fileName);
        logger.info("Exiting uploadFile(byte[], String) method");
        return "File " + fileName + " uploaded as " + fileName;
    }

    private void storeFile(String fileName, byte[] fileContent) {
        // Create a new GCS client
        Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        BlobId blobId;
        BlobInfo blobInfo;
        blobId = BlobId.of(bucketName, fileName);
        String contentType = "application/octet-stream";
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lowerFileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerFileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lowerFileName.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lowerFileName.endsWith(".heic") || lowerFileName.endsWith(".heif")) {
                contentType = "image/heic";
            } else if (lowerFileName.endsWith(".csv")) {
                contentType = "text/csv";
            } else if (lowerFileName.endsWith(".txt")) {
                contentType = "text/plain";
            }
        }
        blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();

        storage.create(blobInfo, fileContent);
    }

    
    /**
     * Downloads a file from Google Cloud Storage and returns its contents as a byte array.
     *
     * @param fileName the name of the file to download
     * @return the contents of the file as a byte array, or null if the file does not exist
     */
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
    @Tool(
        name = "storage_list_files",
        description = "Lists all files in a Google Cloud Storage bucket with a given prefix. " + 
        "The prefix can be an empty string to list all files. If you want to list files in a folder, " +
        "specify the folder name followed by a slash, e.g., 'foldername/'."
    )
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

    /**
     * Returns a list of public URLs for all files in a given folder in the GCS bucket.
     *
     * @param folderName the name of the folder in the bucket
     * @return a list of URLs for each file in the folder
     */
    @Tool(name = "storage_list_file_urls", description = "Returns a list of public URLs for all files in a given folder in the GCS bucket")
    public List<String> listFileUrlsInFolder(String folderName) {
        logger.info("Entering listFileUrlsInFolder method for folder: {}", folderName);
        List<String> urls = new ArrayList<>();
        try {
            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            String prefix = folderName.endsWith("/") ? folderName : folderName + "/";
            for (Blob blob : storage.list(bucketName, BlobListOption.prefix(prefix), BlobListOption.currentDirectory()).iterateAll()) {
                if (!blob.isDirectory()) {
                    String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, blob.getName());
                    urls.add(url);
                    logger.debug("Added URL: {}", url);
                }
            }
        } catch (Exception e) {
            logger.error("Exception occurred while listing file URLs: {}", e.getMessage());
            throw e;
        }
        logger.info("Exiting listFileUrlsInFolder method");
        return urls;
    }

    public Resource getResourceFromGcsUrl(String gcsUrl) {
        logger.info("Entering getResourceFromGcsUrl method with URL: {}", gcsUrl);
        if (gcsUrl == null) {
            logger.error("Null URL provided");
            return null;
        }
        try {
            String bucket;
            String objectName;

            if (gcsUrl.startsWith("gs://")) {
                String withoutPrefix = gcsUrl.substring(5); // remove "gs://"
                int slashIndex = withoutPrefix.indexOf('/');
                if (slashIndex < 0) {
                    logger.error("GCS URL missing object name: {}", gcsUrl);
                    return null;
                }
                bucket = withoutPrefix.substring(0, slashIndex);
                objectName = withoutPrefix.substring(slashIndex + 1);
            } else if (gcsUrl.startsWith("https://storage.googleapis.com/")) {
                String withoutPrefix = gcsUrl.substring("https://storage.googleapis.com/".length());
                int slashIndex = withoutPrefix.indexOf('/');
                if (slashIndex < 0) {
                    logger.error("HTTP URL missing object name: {}", gcsUrl);
                    return null;
                }
                bucket = withoutPrefix.substring(0, slashIndex);
                objectName = withoutPrefix.substring(slashIndex + 1);
            } else {
                logger.error("Invalid GCS or HTTP URL: {}", gcsUrl);
                return null;
            }

            Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
            Blob blob = storage.get(BlobId.of(bucket, objectName));
            if (blob == null) {
                logger.error("Blob not found for URL: {}", gcsUrl);
                return null;
            }
            byte[] content = blob.getContent();
            logger.info("Exiting getResourceFromGcsUrl method");
            return new ByteArrayResource(content);
        } catch (Exception e) {
            logger.error("Exception in getResourceFromGcsUrl: {}", e.getMessage());
            return null;
        }
    }
}
