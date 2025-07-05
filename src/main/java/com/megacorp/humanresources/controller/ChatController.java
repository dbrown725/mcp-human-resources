package com.megacorp.humanresources.controller;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import com.megacorp.humanresources.service.EmployeeService;
import com.megacorp.humanresources.service.ExternalMCPService;

import io.modelcontextprotocol.client.McpSyncClient;

@RestController
public class ChatController {
    
    private final ChatClient chatClient;

    private final ExternalMCPService externalMCPService;

    private final EmployeeService employeeService;
    /**
     * Constructor for ChatController.
     *
     * @param externalMCPService the service to interact with external MCPs
     * @param employeeService the service to manage employee data
     * @param chatClientBuilder the builder for creating a ChatClient instance
     * @param mcpSyncClients list of MCP sync clients for tool callbacks
     */

    public ChatController(ExternalMCPService externalMCPService, EmployeeService employeeService, ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.externalMCPService = externalMCPService;
        this.employeeService = employeeService;
        this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    }

    @GetMapping("/ai")
    String generationWithTools(@RequestParam(name = "prompt", defaultValue = "Tell me a joke", required = false) String prompt) {
        return this.chatClient.prompt()
            .tools(employeeService, externalMCPService)
            .user(prompt)
            .call()
            .content();
    }
    
}
