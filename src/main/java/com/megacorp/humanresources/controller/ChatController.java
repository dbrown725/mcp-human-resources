package com.megacorp.humanresources.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import com.megacorp.humanresources.service.EmployeeService;
import com.megacorp.humanresources.service.BraveSearchService;

import io.modelcontextprotocol.client.McpSyncClient;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {
    
    private final ChatClient chatClient;

    private final EmployeeService employeeService;

    private final BraveSearchService braveSearchService;
    /**
     * Constructor for ChatController.
     *
     * @param employeeService the service to manage employee data
     * @param chatClientBuilder the builder for creating a ChatClient instance
     * @param mcpSyncClients list of MCP sync clients for tool callbacks
     */

    public ChatController(EmployeeService employeeService, BraveSearchService braveSearchService, ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.employeeService = employeeService;
        this.braveSearchService = braveSearchService;
        this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    }

    @GetMapping("/ai")
    String generationWithTools(@RequestParam(name = "prompt", defaultValue = "Tell me a joke", required = false) String prompt) {
        return this.chatClient.prompt()
            .tools(employeeService, braveSearchService)
            .user(prompt)
            .call()
            .content();
    }


    @GetMapping("/ai/chat-response")
    public ChatResponse chatResponse(
            @RequestParam(value = "prompt", defaultValue = "Tell me a joke") String prompt) {
        return chatClient.prompt()
                .tools(employeeService, braveSearchService)
                .user(prompt)
                .call()
                .chatResponse();
    }
    
    @GetMapping("/ai/stream")
    public Flux<String> stream(
        @RequestParam(value = "prompt", defaultValue = "Tell me a joke") String prompt) {
        return chatClient.prompt()
                .tools(employeeService, braveSearchService)
                .user(prompt)
                .stream()
                .content();
    }
    
}
