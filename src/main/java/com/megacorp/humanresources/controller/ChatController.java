package com.megacorp.humanresources.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.web.bind.annotation.*;

import io.modelcontextprotocol.client.McpSyncClient;

@RestController
public class ChatController {
    
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    }

    @GetMapping("/ai")
    String generation(@RequestParam(name = "prompt", required = false) String prompt) {
        return this.chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
}
