package com.megacorp.humanresources.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

// Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/multimodal/image/ImageDetection.java
@RestController
public class ImageDetectionController {

    private final ChatClient chatClient;
    @Value("classpath:/images/sincerely-media-2UlZpdNzn2w-unsplash.jpg")
    Resource sampleImage;
    @Value("classpath:/images/sales-receipt-sample.jpg")
    Resource sampleReceiptImage;

    public ImageDetectionController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/image-to-text")
    public String image() throws IOException {
        return chatClient.prompt()
                .user(u -> u
                        .text("Can you please explain what you see in the following image?")
                        .media(MimeTypeUtils.IMAGE_JPEG,sampleImage)
                )
                .call()
                .content();
    }

    // Image source: https://coefficient.io/templates/sales-receipt-template
    @GetMapping("/receipt-image-to-text")
    public String recieptImage(@RequestParam(value = "prompt", defaultValue = "What payment method was used?") String prompt) throws IOException {
        return chatClient.prompt()
                .user(u -> u
                        .text(prompt)
                        .media(MimeTypeUtils.IMAGE_JPEG,sampleReceiptImage)
                )
                .call()
                .content();
    }
}

