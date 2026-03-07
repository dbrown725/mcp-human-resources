package com.megacorp.humanresources;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;

import com.megacorp.humanresources.service.EmployeeService;
import com.megacorp.humanresources.service.BraveSearchService;
import com.megacorp.humanresources.service.KeepAliveService;
import com.megacorp.humanresources.service.FileStorageService;
import com.megacorp.humanresources.service.ImageGenerationService;
import com.megacorp.humanresources.service.WeatherService;
import com.megacorp.humanresources.service.EmailService;
import com.megacorp.humanresources.service.AddressService;

import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class HumanresourcesApplication {

	private static final Logger logger = LoggerFactory.getLogger(HumanresourcesApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(HumanresourcesApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider availableTools(ObjectProvider<EmployeeService> employeeService,
			ObjectProvider<AddressService> addressService,
			ObjectProvider<BraveSearchService> braveSearchService,
			ObjectProvider<KeepAliveService> keepAliveService,
			ObjectProvider<FileStorageService> fileStorageService,
			ObjectProvider<ImageGenerationService> imageGenerationService,
			ObjectProvider<WeatherService> weatherService,
			ObjectProvider<EmailService> emailService) {

		List<Object> toolObjects = new ArrayList<>();
		employeeService.ifAvailable(toolObjects::add);
		addressService.ifAvailable(toolObjects::add);
		braveSearchService.ifAvailable(toolObjects::add);
		keepAliveService.ifAvailable(toolObjects::add);
		fileStorageService.ifAvailable(toolObjects::add);
		imageGenerationService.ifAvailable(toolObjects::add);
		weatherService.ifAvailable(toolObjects::add);
		emailService.ifAvailable(toolObjects::add);

		if (toolObjects.isEmpty()) {
			logger.warn("No MCP tool beans available during startup; continuing without method tool callbacks");
		} else {
			logger.info("Registering {} MCP tool beans", toolObjects.size());
		}

		return MethodToolCallbackProvider.builder().toolObjects(toolObjects.toArray()).build();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

}
