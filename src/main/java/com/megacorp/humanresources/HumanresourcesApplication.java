package com.megacorp.humanresources;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.megacorp.humanresources.service.EmployeeService;
import com.megacorp.humanresources.service.BraveSearchService;
import com.megacorp.humanresources.service.EmailService;
import com.megacorp.humanresources.service.KeepAliveService;
import com.megacorp.humanresources.service.FileStorageService;
import com.megacorp.humanresources.service.ImageGenerationService;
import com.megacorp.humanresources.service.WeatherService;

import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class HumanresourcesApplication {

	public static void main(String[] args) {
		SpringApplication.run(HumanresourcesApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider availableTools(EmployeeService employeeService, BraveSearchService braveSearchService,
			KeepAliveService keepAliveService, FileStorageService fileStorageService,
			ImageGenerationService imageGenerationService,
			WeatherService weatherService) {
		return MethodToolCallbackProvider.builder().toolObjects(employeeService, braveSearchService, keepAliveService,
				fileStorageService, imageGenerationService, weatherService).build();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
