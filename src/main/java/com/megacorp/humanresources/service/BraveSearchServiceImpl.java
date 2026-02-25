package com.megacorp.humanresources.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class BraveSearchServiceImpl implements BraveSearchService {

    @Value("${brave.search.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public BraveSearchServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Tool(name = "braveSearch", description = "Searches Brave Search for information and extracts entities.")
    public BraveSearchApiResponse braveSearch(String query) {
        log.debug("Entering braveSearch with query={}", query);

        String apiUrl = "https://api.search.brave.com/res/v1/web/search?q=" + query + "&entity=true";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Subscription-Token", apiKey);
        headers.set("Accept", "application/json");

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        BraveSearchApiResponse response = null;
        try {
            response = restTemplate.exchange(apiUrl, org.springframework.http.HttpMethod.GET,
                    requestEntity, BraveSearchApiResponse.class).getBody();
            int resultCount = response != null && response.getWeb() != null && response.getWeb().getResults() != null
                    ? response.getWeb().getResults().size()
                    : 0;
            log.info("Brave search completed successfully with {} results", resultCount);
        } catch (Exception e) {
            log.error("Brave search call failed for query={}", query, e);
            throw e;
        }

        return response;
    }

    static class BraveSearchApiResponse {
        private Web web;

        public Web getWeb() {
            return web;
        }

        static class Web {
            private List<Result> results;

            public List<Result> getResults() {
                return results;
            }

            public void setResults(List<Result> results) {
                this.results = results;
            }
        }

        static class Result {
            private String title;
            private String url;
            private String description;

            public String getTitle() {
                return title;
            }

            public void setTitle(String title) {
                this.title = title;
            }

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            @Override
            public String toString() {
                return "Result [title=" + title + ", url=" + url + ", description=" + description + "]";
            }
        }

        @Override
        public String toString() {
            return "BraveSearchApiResponse [web=" + web + "]";
        }
    }
}
