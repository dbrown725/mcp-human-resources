package com.megacorp.humanresources.evals;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

// Copied from https://github.com/danvega/spring-ai-workshop/blob/main/src/test/java/dev/danvega/workshop/evals/ChatClientTestConfig.java
@TestConfiguration
class ChatClientTestConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}