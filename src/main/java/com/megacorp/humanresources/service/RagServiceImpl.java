package com.megacorp.humanresources.service;

import com.megacorp.humanresources.model.PolicyRagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Service
class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);
    private static final String DEFAULT_POLICY_PREFIX = "policies/";
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_SIMILARITY_THRESHOLD = 0.6;

    private final ElasticsearchVectorStore vectorStore;

    private final ChatClient ai;

    private final FileStorageService fileStorageService;

    RagServiceImpl(ElasticsearchVectorStore vectorStore, ChatClient.Builder clientBuilder, FileStorageService fileStorageService) {
        this.vectorStore = vectorStore;
        this.ai = clientBuilder.build();
        this.fileStorageService = fileStorageService;
    }

    // public void ingest(Resource path) {
    //     logger.debug("Entering ingest with resource={}", path);
    //     PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(path);
    //     List<Document> batch = new TokenTextSplitter().apply(pdfReader.read());
    //     vectorStore.add(batch);
    //     logger.info("RAG ingest completed with {} document chunks", batch.size());
    // }


    // public String advisedRag(String question) {
    //     logger.debug("Entering advisedRag with question={}", question);
    //     String response = this.ai
    //             .prompt()
    //             .user(question)
    //             .advisors(QuestionAnswerAdvisor.builder(vectorStore).build())
    //             .call()
    //             .content();
    //     logger.info("RAG advised query completed successfully");
    //     return response;
    // }


    // public String directRag(String question) {
    //     logger.debug("Entering directRag with question={}", question);
    //     // Query the vector store for documents related to the question
    //     List<Document> vectorStoreResult =
    //             vectorStore.doSimilaritySearch(SearchRequest.builder().query(question).topK(5)
    //                     .similarityThreshold(0.7).build());

    //     // Merging the documents into a single string
    //     String documents = vectorStoreResult.stream()
    //             .map(Document::getText)
    //             .collect(Collectors.joining(System.lineSeparator()));

    //     // Exit if the vector search didn't find any results
    //     if (documents.isEmpty()) {
    //         logger.warn("No relevant context found for directRag question={}", question);
    //         return "No relevant context found. Please change your question.";
    //     }

    //     // Setting the prompt with the context
    //     String prompt = """
    //             You're assisting with providing answers asked by employees concerning the company's employee code of conflict policy.
    //             Use the information from the DOCUMENTS section to provide accurate answers to the
    //             question in the QUESTION section.
    //             If unsure, simply state that you don't know.
                
    //             DOCUMENTS:
    //             """ + documents
    //             + """
    //             QUESTION:
    //             """ + question;


    //     // Calling the chat model with the question
    //     String response = ai
    //             .prompt()
    //             .user(prompt)
    //             .call()
    //             .content();

    //     logger.info("RAG direct query completed successfully with {} matching documents", vectorStoreResult.size());

    //     return response +
    //             System.lineSeparator() +
    //             "Found at page: " +
    //             // Retrieving the first ranked page number from the document metadata
    //             vectorStoreResult.getFirst().getMetadata().get(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER) +
    //             " of the manual";

    // }

    @Override
    public int ingestPoliciesFromGcs(String prefix) {
        String effectivePrefix = (prefix == null || prefix.isBlank()) ? DEFAULT_POLICY_PREFIX : prefix;
        if (!effectivePrefix.endsWith("/")) {
            effectivePrefix = effectivePrefix + "/";
        }

        logger.info("Starting policy RAG ingest from GCS prefix={}", effectivePrefix);

        List<String> policyPdfPaths = fileStorageService.listFiles(effectivePrefix).stream()
            .filter(path -> path != null && path.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            .toList();

        int totalChunks = 0;
        for (String policyPdfPath : policyPdfPaths) {
            try {
                byte[] fileBytes = fileStorageService.retrieveFile(policyPdfPath);
                if (fileBytes == null || fileBytes.length == 0) {
                    logger.warn("Skipping empty policy pdf in GCS path={}", policyPdfPath);
                    continue;
                }

                PagePdfDocumentReader reader = new PagePdfDocumentReader(new ByteArrayResource(fileBytes));
                List<Document> pages = reader.read();
                List<Document> splitPages = new TokenTextSplitter().apply(pages);

                String policyTitle = extractPolicyTitle(policyPdfPath);
                List<Document> enrichedDocuments = splitPages.stream()
                    .map(doc -> enrichPolicyDocument(doc, policyTitle, policyPdfPath))
                    .toList();

                vectorStore.add(enrichedDocuments);
                totalChunks += enrichedDocuments.size();
                logger.info("Ingested policy {} with {} chunks", policyPdfPath, enrichedDocuments.size());
            } catch (Exception e) {
                logger.error("Failed ingest for policy pdf path={}", policyPdfPath, e);
            }
        }

        logger.info("Completed policy RAG ingest for {} files, {} chunks", policyPdfPaths.size(), totalChunks);
        return totalChunks;
    }

    /**
     * Queries the policy vector store to find relevant HR policies based on a question.
     * 
     * This method performs a similarity search against the vector store to find documents
     * matching the provided question, retrieves supporting context, and generates an answer
     * using an AI model based on the found policy context.
     * 
     * @param question the HR policy question to search for. Used as the query input for
     *                 similarity search against the vector store.
     * @param topK the maximum number of similar documents to retrieve. If null or less than 1,
     *             defaults to {@link #DEFAULT_TOP_K}. Controls the breadth of the search results.
     * @param similarityThreshold the minimum similarity score (0.0 to 1.0) for matching documents.
     *                            If null, defaults to {@link #DEFAULT_SIMILARITY_THRESHOLD}.
     *                            Documents with lower similarity scores will be filtered out.
     * 
     * @return a {@link PolicyRagResponse} containing:
     *         - the AI-generated answer based on policy context
     *         - a list of GCS file paths for matched policy attachments
     *         - a list of matched policy titles
     *         - the concatenated supporting context from matched documents
     *         - the count of matching documents found
     *         
     *         If no relevant policies are found, returns a response with a default message
     *         and empty collections.
     */
    @Override
    public PolicyRagResponse queryPolicies(String question, Integer topK, Double similarityThreshold) {
        int effectiveTopK = topK == null || topK < 1 ? DEFAULT_TOP_K : topK;
        double effectiveSimilarity = similarityThreshold == null ? DEFAULT_SIMILARITY_THRESHOLD : similarityThreshold;

        logger.debug("Querying policy vector store with topK={} similarityThreshold={}", effectiveTopK, effectiveSimilarity);

        List<Document> matches = vectorStore.doSimilaritySearch(SearchRequest.builder()
            .query(question)
            .topK(effectiveTopK)
            .similarityThreshold(effectiveSimilarity)
            .build());

        if (matches == null || matches.isEmpty()) {
            return new PolicyRagResponse(
                "No relevant policy context found.",
                List.of(),
                List.of(),
                "",
                0
            );
        }

        String supportingContext = matches.stream()
            .map(Document::getText)
            .filter(Objects::nonNull)
            .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));

        List<String> attachmentPaths = matches.stream()
            .flatMap(doc -> Stream.of(doc.getMetadata().get("gcsPath")))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .distinct()
            .toList();

        List<String> matchedPolicyTitles = matches.stream()
            .flatMap(doc -> Stream.of(doc.getMetadata().get("policyTitle")))
            .filter(Objects::nonNull)
            .map(Object::toString)
            .distinct()
            .toList();

        String answerPrompt = """
            You are an HR policy assistant. Answer using only the policy context below.
            If context is insufficient, say you are not certain and suggest contacting HR.

            QUESTION:
            %s

            POLICY_CONTEXT:
            %s
            """.formatted(question, supportingContext);

        String answer = ai.prompt()
            .user(answerPrompt)
            .call()
            .content();

        return new PolicyRagResponse(
            answer == null ? "" : answer,
            attachmentPaths,
            matchedPolicyTitles,
            supportingContext,
            matches.size()
        );
    }

    private Document enrichPolicyDocument(Document sourceDoc, String policyTitle, String gcsPath) {
        Map<String, Object> metadata = new HashMap<>(sourceDoc.getMetadata());
        metadata.put("policyTitle", policyTitle);
        metadata.put("gcsPath", gcsPath);
        metadata.put("sourceType", "gcs-policy-pdf");
        return new Document(sourceDoc.getText(), metadata);
    }

    private String extractPolicyTitle(String gcsPath) {
        if (gcsPath == null || gcsPath.isBlank()) {
            return "";
        }

        String title = gcsPath;
        int slash = title.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < title.length()) {
            title = title.substring(slash + 1);
        }

        if (title.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            title = title.substring(0, title.length() - 4);
        }

        if (title.startsWith("policies_")) {
            title = title.substring("policies_".length());
        }

        return title;
    }

}
