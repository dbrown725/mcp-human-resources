package com.megacorp.humanresources.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.megacorp.humanresources.service.WeatherService;

//Based on https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/tools/weather/WeatherController.java
@RestController
public class WeatherController {

    private final ChatClient chatClient;
    private final WeatherService weatherService;

    public WeatherController(ChatClient.Builder builder, WeatherService weatherService) {
        this.chatClient = builder.build();
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/alerts")
    public String getAlerts(@RequestParam String message) {
        return chatClient.prompt()
                .tools(weatherService)
                .user(message)
                .call()
                .content();
    }

    @GetMapping("/weather/forecast")
    public String getForecast(@RequestParam String message) {
        return chatClient.prompt()
                .tools(weatherService)
                .user(message)
                .call()
                .content();
    }

}