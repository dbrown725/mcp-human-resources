package com.megacorp.humanresources.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.megacorp.humanresources.model.Sentiment;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ChatClient chatClient;

    public ReviewServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultOptions(VertexAiGeminiChatOptions.builder().temperature(0.1d).build())
                .build();
    }

    public Sentiment classifySentiment(String review) {
        logger.debug("Entering classifySentiment with reviewLength={}", review == null ? 0 : review.length());
        String systemPrompt = """
                Classify the sentiment of the following text as POSITIVE, NEGATIVE, or NEUTRAL. \
                Your response must be only one of these three words.""";

        Sentiment sentiment = chatClient.prompt()
                .system(systemPrompt)
                .user(review)
                .call()
                .entity(Sentiment.class);
        logger.info("Sentiment classification completed successfully");
        return sentiment;
    }

}
