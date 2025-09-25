package com.megacorp.humanresources.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import com.megacorp.humanresources.service.FileStorageService;
import com.megacorp.humanresources.service.ImageSummaryService;
import com.megacorp.humanresources.service.ImageGenerationService;

@RestController
public class ImageController {

    private final ChatClient chatClient;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ImageSummaryService imageSummaryService;

    @Autowired
    private ImageGenerationService imageGenerationService;

    // abc_hardware_store.png, intellicare_solutions.jpeg, techwave_solutions.jpg, xyz_bookstore.webp, cash_receipt.heic, abc_electronics.heif
    @Value("classpath:/images/abc_electronics.heif")
    Resource sampleReceiptImage;

    public ImageController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/multimodal/image/ImageDetection.java
    // Image source: https://coefficient.io/templates/sales-receipt-template
    @GetMapping("/receipt-image-to-text")
    public String receiptImage(@RequestParam(value = "prompt", defaultValue = "What payment method was used?") String prompt) throws IOException {
        String filename = sampleReceiptImage.getFilename();
        String mimeType = resolveMimeType(filename);
        MimeType resolvedMineType = MimeType.valueOf(mimeType);
        // gs://mcp-human-resources/expense_receipts/abc_electronics.heif
        Resource imageUrl = fileStorageService.getResourceFromGcsUrl("https://storage.googleapis.com/mcp-human-resources/expense_receipts/abc_electronics.heif");
        return chatClient.prompt()
                .user(u -> u
                        .text(prompt)
                        .media(resolvedMineType, imageUrl)
                )
                .call()
                .content();
    }

    @GetMapping("/summarize-images-in-folder")
    public java.util.List<String> summarizeImagesInFolder(@RequestParam("folder") String folderName) throws IOException {
        return imageSummaryService.summarizeImagesInFolder(folderName);
    }

    @GetMapping("/generate-expense-report")
    private String generateExpenseReportFromImages(@RequestParam("folder") String folderName) throws IOException {
        return imageSummaryService.generateExpenseReportFromImages(folderName);
    }

    private String resolveMimeType(String filename) {
        String mimeType = "";
        if (filename != null) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            switch (ext) {
                case "jpg":
                case "jpeg":
                    mimeType = MimeTypeUtils.IMAGE_JPEG_VALUE;
                    break;
                case "png":
                    mimeType = MimeTypeUtils.IMAGE_PNG_VALUE;
                    break;
                case "gif":
                    mimeType = MimeTypeUtils.IMAGE_GIF_VALUE;
                    break;
                case "webp":
                    mimeType = "image/webp";
                    break;
                case "heic":
                    mimeType = "image/heic";
                    break;
                case "heif":
                    mimeType = "image/heif";
                    break;
            }
        }
        return mimeType;
    }

    @GetMapping("/generate-image")
    public String generateImage(
        @RequestParam("prompt") String prompt,
        @RequestParam("outputImageRootName") String outputImageRootName) throws IOException {
        return imageGenerationService.generateImage(prompt, outputImageRootName);
    }
}
