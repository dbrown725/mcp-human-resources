package com.megacorp.humanresources.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.megacorp.humanresources.service.WeatherService;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

//Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/tools/weather/WeatherController.java
@RestController
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final ChatClient chatClient;
    private final WeatherService weatherService;

    public WeatherController(ChatClient.Builder builder, WeatherService weatherService, CallAdvisor chatClientLoggingAdvisor) {
        this.chatClient = builder.defaultAdvisors(chatClientLoggingAdvisor).build();
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/alerts")
    public String getAlerts(@RequestParam String message) {
        log.debug("Entering getAlerts with message={}", message);
        String alerts = chatClient.prompt()
                .tools(weatherService)
                .user(message)
                .call()
                .content();
        log.info("Weather alerts response generated successfully");
        return alerts;
    }

    @GetMapping("/weather/forecast")
    public String getForecast(@RequestParam String message) {
        log.debug("Entering getForecast with message={}", message);
        String forecast = chatClient.prompt()
                .tools(weatherService)
                .user(message)
                .call()
                .content();
        log.info("Weather forecast response generated successfully");
        return forecast;
    }

}