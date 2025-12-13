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

import com.megacorp.humanresources.service.ImageSummaryService;
import com.megacorp.humanresources.service.ImageGenerationService;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

@RestController
public class ImageController {

    private final ChatClient chatClient;

    @Autowired
    private ImageSummaryService imageSummaryService;

    @Autowired
    private ImageGenerationService imageGenerationService;

    // abc_hardware_store.png, intellicare_solutions.jpeg, techwave_solutions.jpg, xyz_bookstore.webp
    // .heic and .heif mime types no longer supported by gemini-2.5.flash? cash_receipt.heic, abc_electronics.heif
    @Value("classpath:/images/xyz_bookstore.webp")
    Resource sampleReceiptImage;

    public ImageController(ChatClient.Builder builder, CallAdvisor chatClientLoggingAdvisor) {
        this.chatClient = builder.defaultAdvisors(chatClientLoggingAdvisor).build();
    }

    // Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/multimodal/image/ImageDetection.java
    // Image source: https://coefficient.io/templates/sales-receipt-template
    @GetMapping("/receipt-image-to-text")
    public String receiptImage(@RequestParam(value = "prompt", defaultValue = "What payment method was used?") String prompt) throws IOException {
        String filename = sampleReceiptImage.getFilename();
        String mimeType = resolveMimeType(filename);
        MimeType resolvedMineType = MimeType.valueOf(mimeType);
        // gs://mcp-human-resources/expense_receipts/abc_electronics.heif
        // Resource imageUrl = fileStorageService.getResourceFromGcsUrl("https://storage.googleapis.com/mcp-human-resources/expense_receipts/20250831_20250913/abc_hardware_store.png");
        return chatClient.prompt()
                .user(u -> u
                        .text(prompt)
                        .media(resolvedMineType, sampleReceiptImage)
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
        @RequestParam(value = "optionalInputImageNames", required = false) String[] optionalInputImageNames,
        @RequestParam("outputImageRootName") String outputImageRootName) throws IOException {
        return imageGenerationService.generateImage(prompt, optionalInputImageNames, outputImageRootName);
    }

    @GetMapping("/generate-employee-badge")
    public String generateEmployeeBadge(
        @RequestParam("firstName") String firstName,
        @RequestParam("lastName") String lastName,
        @RequestParam("employeeNumber") String employeeNumber,
        @RequestParam("existingEmployeeImageName") String existingEmployeeImageName) throws IOException {
        String employeeBadgeTemplateName = "original_images/employeeBadgeTemplate.png";    
        String[] optionalInputImageNames = new String[] { employeeBadgeTemplateName, existingEmployeeImageName };
        String outputImageRootName = String.format("%s_%s_%s_badge", firstName.toLowerCase(), lastName.toLowerCase(), employeeNumber);
        String prompt = String.format(
                "Edit the employee badge template image by replacing the placeholder person image %s with the photo from %s. Position the photo so that the person's face and both hands are fully visible and properly aligned on the badge. Insert the employee name '%s' and employee number '%s' into the corresponding text fields on the template. Preserve the original style, colors, fonts, and layout of the badge without any additional changes.",
                employeeBadgeTemplateName, existingEmployeeImageName, firstName + "_" + lastName, employeeNumber);
        return imageGenerationService.generateImage(prompt, optionalInputImageNames, outputImageRootName);
    }
}
