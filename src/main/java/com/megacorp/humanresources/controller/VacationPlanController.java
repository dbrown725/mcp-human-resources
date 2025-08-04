package com.megacorp.humanresources.controller;

import com.megacorp.humanresources.model.Itinerary;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/output/VacationPlan.java
@RestController
public class VacationPlanController {

    private final ChatClient chatClient;

    public VacationPlanController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/vacation/unstructured")
    public String vacationUnstructured() {
        return chatClient.prompt()
                .user("What's a good vacation plan while I'm in Montreal CA for 4 days?")
                .call()
                .content();
    }

    @GetMapping("/vacation/structured")
    public Itinerary vacationStructured(@RequestParam(value = "destination", defaultValue = "Cleveland, OH") String destination) {
        return chatClient.prompt()
                .user(u -> {
                    u.text("What's a good vacation plan while I'm in {destination} for 3 days?");
                    u.param("destination", destination);
                })
                .call()
                .entity(Itinerary.class);
    }

}