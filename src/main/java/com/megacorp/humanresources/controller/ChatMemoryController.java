package com.megacorp.humanresources.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

/**
 * Simple controller that demonstrates interacting with a ChatClient configured
 * with a ChatMemory advisor.
 */
@RestController
public class ChatMemoryController {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryController.class);

    private final ChatClient chatClient;

    public ChatMemoryController(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory,
                                CallAdvisor chatClientLoggingAdvisor) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        chatClientLoggingAdvisor
                )
                .build();
    }

     // https://docs.spring.io/spring-ai/reference/api/chat-memory.html
    @GetMapping("/memory")
    public String chatWithMemory(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        log.debug("Received memory chat message: {}", message);
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }

}
