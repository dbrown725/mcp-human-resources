package com.megacorp.humanresources.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

// Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/multimodal/image/ImageDetection.java
@RestController
public class ImageDetectionController {

    private final ChatClient chatClient;

    // abc_hardware_store.png, intellicare_solutions.jpeg, techwave_solutions.jpg, xyz_bookstore.webp, cash_receipt.heic, abc_electronics.heif
    @Value("classpath:/images/abc_electronics.heif")
    Resource sampleReceiptImage;

    public ImageDetectionController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // Image source: https://coefficient.io/templates/sales-receipt-template
    @GetMapping("/receipt-image-to-text")
    public String recieptImage(@RequestParam(value = "prompt", defaultValue = "What payment method was used?") String prompt) throws IOException {
        String filename = sampleReceiptImage.getFilename();
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
        MimeType resolvedMineType = MimeType.valueOf(mimeType);
        return chatClient.prompt()
                .user(u -> u
                        .text(prompt)
                        .media(resolvedMineType, sampleReceiptImage)
                )
                .call()
                .content();
    }
}

