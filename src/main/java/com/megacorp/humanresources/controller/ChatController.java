package com.megacorp.humanresources.controller;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
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

    private final ChatClient secondaryChatClient;

    private final ChatClient tertiaryChatClient;

    /**
     * Constructor for ChatController.
     *
     * @param employeeService the service to manage employee data
     * @param braveSearchService the service used to query Brave search tool
     * @param chatClientBuilder the builder for creating the primary ChatClient instance
     * @param mcpSyncClients list of MCP sync clients used for tool callbacks
     * @param chatClientLoggingAdvisor advisor applied to the ChatClient for logging/advice
     * @param secondaryChatClient an alternate ChatClient instance qualified as "secondaryChatClient"
     * @param tertiaryChatClient an alternate ChatClient instance qualified as "tertiaryChatClient"
     */
    public ChatController(EmployeeService employeeService, BraveSearchService braveSearchService, 
        ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients,
        CallAdvisor chatClientLoggingAdvisor, @org.springframework.beans.factory.annotation.Qualifier("secondaryChatClient") ChatClient secondaryChatClient,
        @org.springframework.beans.factory.annotation.Qualifier("tertiaryChatClient") ChatClient tertiaryChatClient) {
        this.employeeService = employeeService;
        this.braveSearchService = braveSearchService;
        this.secondaryChatClient = secondaryChatClient;
        this.tertiaryChatClient = tertiaryChatClient;
        this.chatClient = chatClientBuilder.defaultAdvisors(chatClientLoggingAdvisor)
                .defaultToolCallbacks(SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build())
                .build();
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

    @GetMapping("/models/stuff-the-prompt")
    public String modelsWithData() {
        String system = """
            If you are asked about up to date language models and their context window here is some information to help you with your response:
            [
              {
                "model": "Llama 4 Scout",
                "company": "Meta",
                "context_window_tokens": 10000000,
                "notes": "10 million‑token window on a single GPU – ideal for full‑book summarisation, long video/audio transcripts and on‑device multimodal workflows."
              },
              {
                "model": "Claude Sonnet 4",
                "company": "Anthropic",
                "context_window_tokens": 1000000,
                "notes": "1 million‑token window for complex multimodal tasks and large‑scale code‑base comprehension."
              },
              {
                "model": "Gemini 2.5 Flash",
                "company": "Google",
                "context_window_tokens": 1000000,
                "notes": "1 million‑token window, focused on fast inference for enterprise document analysis."
              },
              {
                "model": "Gemini 2.5 Pro",
                "company": "Google",
                "context_window_tokens": 1000000,
                "notes": "1 million‑token window, tuned for high‑quality reasoning and multimodal generation."
              },
              {
                "model": "GPT‑4.1",
                "company": "OpenAI",
                "context_window_tokens": 1000000,
                "notes": "1 million‑token window, the latest iteration of the GPT‑4 family."
              },
              {
                "model": "Llama 4 Maverick",
                "company": "Meta",
                "context_window_tokens": 1000000,
                "notes": "1 million‑token window, positioned for enterprise‑grade document analysis."
              },
              {
                "model": "GPT‑4o",
                "company": "OpenAI",
                "context_window_tokens": 128000,
                "notes": "128 k token window, strong for long‑form documents, code generation and retrieval‑augmented tasks."
              },
              {
                "model": "Mistral Large 2",
                "company": "Mistral AI",
                "context_window_tokens": 128000,
                "notes": "128 k token window, balances performance and efficiency for a wide range of applications."
              },
              {
                "model": "DeepSeek R1",
                "company": "DeepSeek AI",
                "context_window_tokens": 128000,
                "notes": "128 k token window, reasoning‑focused model with strong math and coding abilities."
              },
              {
                "model": "DeepSeek V3",
                "company": "DeepSeek AI",
                "context_window_tokens": 128000,
                "notes": "128 k token window, multimodal capabilities and high‑throughput inference."
              }
            ]
            """;
        return chatClient.prompt()
                .user("Can you give me an up to date list of popular large language models and their current context window size")
                .system(system)
                .call()
                .content();
    }

    @GetMapping("/ai/model/secondary")
    public String generatewithModelTwo(@RequestParam(value = "prompt", defaultValue = "Tell me a joke") String prompt) {
        return secondaryChatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    @GetMapping("/ai/model/tertiary")
    public String generate(@RequestParam(value = "prompt", defaultValue = "Tell me a joke") String prompt) {
        return tertiaryChatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
    
}
