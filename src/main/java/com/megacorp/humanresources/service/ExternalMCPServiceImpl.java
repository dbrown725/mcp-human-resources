package com.megacorp.humanresources.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megacorp.humanresources.model.WebSearchResult;

import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ExternalMCPServiceImpl implements ExternalMCPService {

    private final ChatClient chatClient;

    private static final Logger logger = LoggerFactory.getLogger(ExternalMCPServiceImpl.class);

    public ExternalMCPServiceImpl(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {
        this.chatClient = chatClientBuilder.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients)).build();
    }

    /**
     * Performs a web search using the given search term and returns the results as a JSON string.
     *
     * @param searchTerm the search query to use for retrieving web results
     * @return a JSON string representing a list of WebSearchResult objects ordered by relevance
     */
    @Tool(
		name = "search_web",
		description = "Search the web based on input search term. Returns a list of search results, in json format, in order of relevance." +
        " Each search result contains a String rank, String title, String URL, and String snippet."
	)
    public String searchWeb(String searchTerm) {
        logger.info("Enter searchWeb(String searchTerm) with searchTerm: {}", searchTerm);
        logger.debug("searchWeb input - searchTerm: {}", searchTerm);

        ParameterizedTypeReference<List<WebSearchResult>> resultListType = new ParameterizedTypeReference<List<WebSearchResult>>() {};
        
        List<WebSearchResult> webSearchResultList = this.chatClient.prompt()
            .user(searchTerm)
            .call()
            .entity(resultListType);

        ObjectMapper mapper = new ObjectMapper();
        String json = ""; 
        try {
            json = mapper.writeValueAsString(webSearchResultList);
        } catch (Exception e) {
            logger.error("Error serializing webSearchResultList to JSON", e);
        }

        logger.info("Exit searchWeb(String searchTerm) with content: {}", json);
        return json;
    }

    
}
