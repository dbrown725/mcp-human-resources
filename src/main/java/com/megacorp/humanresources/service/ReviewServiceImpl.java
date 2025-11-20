package com.megacorp.humanresources.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.stereotype.Service;
import com.megacorp.humanresources.model.Sentiment;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ChatClient chatClient;

    public ReviewServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultOptions(VertexAiGeminiChatOptions.builder().temperature(0.1d).build())
                .build();
    }

    public Sentiment classifySentiment(String review) {
        String systemPrompt = """
                Classify the sentiment of the following text as POSITIVE, NEGATIVE, or NEUTRAL. \
                Your response must be only one of these three words.""";

        return chatClient.prompt()
                .system(systemPrompt)
                .user(review)
                .call()
                .entity(Sentiment.class);
    }

}
