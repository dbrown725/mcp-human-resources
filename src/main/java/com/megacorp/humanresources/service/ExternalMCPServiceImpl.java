package com.megacorp.humanresources.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExternalMCPServiceImpl implements ExternalMCPService {

    private final ChatClient chatClient;

    private static final Logger logger = LoggerFactory.getLogger(EmployeeServiceImpl.class);

    public ExternalMCPServiceImpl(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    }
    /**
     * This method allows the AI to search the web based on a given search term.
     * It returns a list of search results in order of relevance.
     *
     * @param searchTerm The term to search for on the web.
     * @return A string containing the search results.
     */
    @Tool(
		name = "search_web",
		description = "Search the web based on input search term. Returns a list of search results in order of relevance."
	)
    public String searchWeb(String searchTerm) {
        logger.info("Enter searchWeb(String searchTerm) with searchTerm: {}", searchTerm);
        logger.debug("searchWeb input - searchTerm: {}", searchTerm);
        String content = this.chatClient.prompt()
            .user(searchTerm)
            .call()
            .content();
        logger.info("Exit searchWeb(String searchTerm) with content: {}", content);
        return content;
    }

    
}
