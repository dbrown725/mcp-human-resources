package com.megacorp.humanresources.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.megacorp.humanresources.service.ExternalMCPService;

import io.modelcontextprotocol.client.McpSyncClient;

@RestController
public class ChatController {
    
    // private final ChatClient chatClient;

    private final ExternalMCPService externalMCPService;

    public ChatController(ExternalMCPService externalMCPService) {
        this.externalMCPService = externalMCPService;
    }

    // public ChatController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
    //     this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    // }

    // @GetMapping("/ai")
    // String generation(@RequestParam(name = "prompt", required = false) String prompt) {
    //     return this.chatClient.prompt()
    //         .user(prompt)
    //         .call()
    //         .content();
    // }

    @GetMapping("/ai")
    String generation(@RequestParam(name = "prompt", required = false) String prompt) {
        return externalMCPService.searchWeb(prompt);
    }


    
}
