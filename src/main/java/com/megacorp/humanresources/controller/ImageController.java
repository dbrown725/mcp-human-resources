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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import com.megacorp.humanresources.service.ImageSummaryService;
import com.megacorp.humanresources.service.ImageGenerationService;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

@RestController
public class ImageController {

    private static final Logger log = LoggerFactory.getLogger(ImageController.class);

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
        log.debug("Entering receiptImage with prompt={}", prompt);
        String filename = sampleReceiptImage.getFilename();
        String mimeType = resolveMimeType(filename);
        if (mimeType.isBlank()) {
            log.warn("Unable to resolve MIME type for sample receipt image filename={}", filename);
        }
        MimeType resolvedMineType = MimeType.valueOf(mimeType);
        // gs://mcp-human-resources/expense_receipts/abc_electronics.heif
        // Resource imageUrl = fileStorageService.getResourceFromGcsUrl("https://storage.googleapis.com/mcp-human-resources/expense_receipts/20250831_20250913/abc_hardware_store.png");
        String content = chatClient.prompt()
                .user(u -> u
                        .text(prompt)
                        .media(resolvedMineType, sampleReceiptImage)
                )
                .call()
                .content();
        log.info("Generated receipt image summary for filename={}", filename);
        return content;
    }

    @GetMapping("/summarize-images-in-folder")
    public java.util.List<String> summarizeImagesInFolder(@RequestParam("folder") String folderName) throws IOException {
        log.debug("Entering summarizeImagesInFolder with folder={}", folderName);
        java.util.List<String> summaries = imageSummaryService.summarizeImagesInFolder(folderName);
        log.info("Generated {} image summaries for folder={}", summaries.size(), folderName);
        return summaries;
    }

    @GetMapping("/generate-expense-report")
    private String generateExpenseReportFromImages(@RequestParam("folder") String folderName) throws IOException {
        log.debug("Entering generateExpenseReportFromImages with folder={}", folderName);
        String report = imageSummaryService.generateExpenseReportFromImages(folderName);
        log.info("Generated expense report for folder={}", folderName);
        return report;
    }

    private String resolveMimeType(String filename) {
        log.trace("Resolving MIME type for filename={}", filename);
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
                default:
                    log.warn("Unsupported image extension received while resolving MIME type: {}", ext);
            }
        }
        log.trace("Resolved MIME type {} for filename={}", mimeType, filename);
        return mimeType;
    }

    @GetMapping("/generate-image")
    public String generateImage(
        @RequestParam("prompt") String prompt,
        @RequestParam(value = "optionalInputImageNames", required = false) String[] optionalInputImageNames,
        @RequestParam("outputImageRootName") String outputImageRootName) throws IOException {
        log.debug("Entering generateImage with outputImageRootName={} optionalInputImageCount={}",
                outputImageRootName, optionalInputImageNames == null ? 0 : optionalInputImageNames.length);
        String generatedImage = imageGenerationService.generateImage(prompt, optionalInputImageNames, outputImageRootName);
        log.info("Generated image successfully with outputImageRootName={}", outputImageRootName);
        return generatedImage;
    }

    @GetMapping("/generate-employee-badge")
    public String generateEmployeeBadge(
        @RequestParam("firstName") String firstName,
        @RequestParam("lastName") String lastName,
        @RequestParam("employeeNumber") String employeeNumber,
        @RequestParam("existingEmployeeImageName") String existingEmployeeImageName) throws IOException {
        log.debug("Entering generateEmployeeBadge for employeeNumber={} existingEmployeeImageName={}", employeeNumber, existingEmployeeImageName);
        String employeeBadgeTemplateName = "original_images/employeeBadgeTemplate.png";    
        String[] optionalInputImageNames = new String[] { employeeBadgeTemplateName, existingEmployeeImageName };
        String outputImageRootName = String.format("%s_%s_%s_badge", firstName.toLowerCase(), lastName.toLowerCase(), employeeNumber);
        String prompt = String.format(
                "Edit the employee badge template image by replacing the placeholder person image %s with the photo from %s. Position the photo so that the person's face and both hands are fully visible and properly aligned on the badge. Insert the employee name '%s' and employee number '%s' into the corresponding text fields on the template. Preserve the original style, colors, fonts, and layout of the badge without any additional changes.",
                employeeBadgeTemplateName, existingEmployeeImageName, firstName + "_" + lastName, employeeNumber);
        String badgeImage = imageGenerationService.generateImage(prompt, optionalInputImageNames, outputImageRootName);
        log.info("Generated employee badge for employeeNumber={} outputImageRootName={}", employeeNumber, outputImageRootName);
        return badgeImage;
    }
}
