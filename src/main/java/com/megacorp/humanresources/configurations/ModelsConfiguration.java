package com.megacorp.humanresources.configurations;

import com.megacorp.humanresources.advisors.ChatClientLoggingAdvisor;
import com.megacorp.humanresources.service.EmployeeService;
import com.megacorp.humanresources.service.BenefitsService;
import com.megacorp.humanresources.service.BraveSearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ModelsConfiguration {

    @Bean
    @Primary
    EmbeddingModel embeddingModel(@Qualifier("textEmbedding") EmbeddingModel textEmbedding) {
        return textEmbedding;
    }

	@Bean
	@Primary
	public ChatModel chatModel(@Qualifier("vertexAiGeminiChat") ChatModel vertexAiGeminiChat) {
		return vertexAiGeminiChat;
	}

    // ********************************************************
    // *** Beans related to secondary model and chat client ***
    // ********************************************************
	@Bean
	public OpenAiApi openAiApi(
		@Value("${spring.ai.openai.api-key}") String apiKey,
		@Value("${spring.ai.openai.base-url}") String baseUrl
	) {
		return OpenAiApi.builder()
		.apiKey(apiKey)
		.baseUrl(baseUrl)
		.build();
	}

	@Bean
	public OpenAiChatModel openAiChatModel(OpenAiApi openAiApi,
		@Value("${spring.ai.openai.chat.options.model}") String modelName) {
		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
			.model(modelName)
			.build();
		return OpenAiChatModel.builder()
			.openAiApi(openAiApi)
			.defaultOptions(chatOptions)
			.build();
	}

	@Bean(
		name = "secondaryModel"
	)
	ChatModel ChatModelSecondary(
		OpenAiApi openAiApi,
		OpenAiChatModel openAiChatModel
	) {
		return OpenAiChatModel.builder()
		.openAiApi(openAiApi)
		.defaultOptions(openAiChatModel.getDefaultOptions().copy())
		.build();
	}

	@Bean(
		name = "secondaryChatClient"
	)
	ChatClient secondaryChatClient(
		@Qualifier("secondaryModel") ChatModel secondaryChatModel,
		ChatClientLoggingAdvisor chatClientLoggingAdvisor,
		EmployeeService employeeService,
		BenefitsService benefitsService,
		BraveSearchService braveSearchService
	) {
		return ChatClient.builder(secondaryChatModel)
			.defaultAdvisors(chatClientLoggingAdvisor)
			.defaultTools(employeeService, benefitsService, braveSearchService)
			.build();
	}


    // ****************************************************
    // *** Beans related to judge model and chat client ***
    // ****************************************************
	@Bean
	public OpenAiApi openAiApiJudge(
		@Value("${spring.ai.openai.api-key-judge}") String apiKey,
		@Value("${spring.ai.openai.base-url-judge}") String baseUrl
	) {
		return OpenAiApi.builder()
		.apiKey(apiKey)
		.baseUrl(baseUrl)
		.build();
	}

	@Bean
	public OpenAiChatModel openAiChatModelJudge(OpenAiApi openAiApiJudge,
		@Value("${spring.ai.openai.chat.options.model-judge}") String modelName) {
		OpenAiChatOptions chatOptions = OpenAiChatOptions.builder()
			.model(modelName)
			.temperature(0.0)
			.build();
		return OpenAiChatModel.builder()
			.openAiApi(openAiApiJudge)
			.defaultOptions(chatOptions)
			.build();
	}

	@Bean(
		name = "judgeModel"
	)
	ChatModel chatModelJudge(
		OpenAiApi openAiApiJudge,
		OpenAiChatModel openAiChatModelJudge
	) {
		return OpenAiChatModel.builder()
		.openAiApi(openAiApiJudge)
		.defaultOptions(openAiChatModelJudge.getDefaultOptions().copy())
		.build();
	}

	@Bean(
		name = "judgeChatClient"
	)
	ChatClient judgeChatClient(
		@Qualifier("judgeModel") ChatModel judgeChatModel,
		ChatClientLoggingAdvisor chatClientLoggingAdvisor,
		EmployeeService employeeService,
		BenefitsService benefitsService,
		BraveSearchService braveSearchService
	) {
		return ChatClient.builder(judgeChatModel)
			.defaultAdvisors(chatClientLoggingAdvisor)
			.defaultTools(employeeService, benefitsService, braveSearchService)
			.build();
	}

    
}
