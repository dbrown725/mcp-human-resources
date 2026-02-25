package com.megacorp.humanresources.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import com.megacorp.humanresources.advisors.ChatClientLoggingAdvisor;
import com.megacorp.humanresources.advisors.SelfRefineEvaluationAdvisor;



@Service
public class EmployeeOnboardingServiceImpl implements EmployeeOnboardingService {

	private static final Logger logger = LoggerFactory.getLogger(EmployeeOnboardingServiceImpl.class);

    private final ChatModel secondaryChatModel;

	// Tertiary model for evaluation advisor (the Judge LLM)
    private final ChatModel tertiaryChatModel;

    public EmployeeOnboardingServiceImpl(@Qualifier("secondaryModel") ChatModel secondaryChatModel,
        @Qualifier("tertiaryModel") ChatModel tertiaryChatModel) {
        this.secondaryChatModel = secondaryChatModel;
        this.tertiaryChatModel = tertiaryChatModel;
    }

    @Override
    public String generateWelcomeMessage(String employeeName, String position, String startDate) {
		logger.debug("Entering generateWelcomeMessage with employeeName={} position={} startDate={}", employeeName, position, startDate);
        String prompt = String.format("Generate a warm and engaging welcome message for a new employee named %s who is joining as a %s starting on %s. The message should highlight the company's values and culture, and encourage the new hire to feel excited about their new role.", 
            employeeName, position, startDate);
            
        			ChatClient chatClient = ChatClient.builder(secondaryChatModel) // @formatter:off
					// .defaultTools(new MyTools())
					.defaultAdvisors(
						
						SelfRefineEvaluationAdvisor.builder()
							.chatClientBuilder(ChatClient.builder(tertiaryChatModel)) // The Judge LLM ChatClient
							.maxRepeatAttempts(15)
							.successRating(4)
							.order(0)
							.build(),
						
						new ChatClientLoggingAdvisor(2))
				.build(); 
				
				var answer = chatClient
					.prompt(prompt)
					.call()
					.content();

				// @formatter:on

			logger.info("Generated onboarding welcome message for employeeName={}", employeeName);

        return answer;
    }

}