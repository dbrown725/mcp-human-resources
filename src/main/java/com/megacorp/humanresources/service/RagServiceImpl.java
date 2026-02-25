package com.megacorp.humanresources.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.stream.Collectors;

@Service
class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    private final ElasticsearchVectorStore vectorStore;

    private final ChatClient ai;

    RagServiceImpl(ElasticsearchVectorStore vectorStore, ChatClient.Builder clientBuilder) {
        this.vectorStore = vectorStore;
        this.ai = clientBuilder.build();
    }

    public void ingest(Resource path) {
        logger.debug("Entering ingest with resource={}", path);
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(path);
        List<Document> batch = new TokenTextSplitter().apply(pdfReader.read());
        vectorStore.add(batch);
        logger.info("RAG ingest completed with {} document chunks", batch.size());
    }


    public String advisedRag(String question) {
        logger.debug("Entering advisedRag with question={}", question);
        String response = this.ai
                .prompt()
                .user(question)
                .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .call()
                .content();
        logger.info("RAG advised query completed successfully");
        return response;
    }


    public String directRag(String question) {
        logger.debug("Entering directRag with question={}", question);
        // Query the vector store for documents related to the question
        List<Document> vectorStoreResult =
                vectorStore.doSimilaritySearch(SearchRequest.builder().query(question).topK(5)
                        .similarityThreshold(0.7).build());

        // Merging the documents into a single string
        String documents = vectorStoreResult.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        // Exit if the vector search didn't find any results
        if (documents.isEmpty()) {
            logger.warn("No relevant context found for directRag question={}", question);
            return "No relevant context found. Please change your question.";
        }

        // Setting the prompt with the context
        String prompt = """
                You're assisting with providing answers asked by employees concerning the company's employee code of conflict policy.
                Use the information from the DOCUMENTS section to provide accurate answers to the
                question in the QUESTION section.
                If unsure, simply state that you don't know.
                
                DOCUMENTS:
                """ + documents
                + """
                QUESTION:
                """ + question;


        // Calling the chat model with the question
        String response = ai
                .prompt()
                .user(prompt)
                .call()
                .content();

        logger.info("RAG direct query completed successfully with {} matching documents", vectorStoreResult.size());

        return response +
                System.lineSeparator() +
                "Found at page: " +
                // Retrieving the first ranked page number from the document metadata
                vectorStoreResult.getFirst().getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER) +
                " of the manual";

    }

}
