package com.megacorp.humanresources.controller;

import com.megacorp.humanresources.model.Itinerary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;

// https://github.com/danvega/spring-ai-workshop/blob/main/src/main/java/dev/danvega/workshop/output/VacationPlan.java
@RestController
public class VacationPlanController {

    private static final Logger log = LoggerFactory.getLogger(VacationPlanController.class);

    private final ChatClient chatClient;

    public VacationPlanController(ChatClient.Builder builder,
        CallAdvisor chatClientLoggingAdvisor
    ) {
        this.chatClient = builder.defaultAdvisors(chatClientLoggingAdvisor).build();
    }

    @GetMapping("/vacation/unstructured")
    public String vacationUnstructured() {
        log.debug("Entering vacationUnstructured");
        String itinerary = chatClient.prompt()
                .user("What's a good vacation plan while I'm in Montreal CA for 4 days?")
                .call()
                .content();
        log.info("Generated unstructured vacation plan");
        return itinerary;
    }

    @GetMapping("/vacation/structured")
    public Itinerary vacationStructured(@RequestParam(value = "destination", defaultValue = "Cleveland, OH") String destination) {
        log.debug("Entering vacationStructured with destination={}", destination);
        Itinerary itinerary = chatClient.prompt()
                .user(u -> {
                    u.text("What's a good vacation plan while I'm in {destination} for 3 days?");
                    u.param("destination", destination);
                })
                .call()
                .entity(Itinerary.class);
        log.info("Generated structured vacation plan for destination={}", destination);
        return itinerary;
    }

}