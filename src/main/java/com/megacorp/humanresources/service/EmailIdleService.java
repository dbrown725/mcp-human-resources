package com.megacorp.humanresources.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;

import com.megacorp.humanresources.model.EmailMessage;
import com.megacorp.humanresources.model.PolicyRagResponse;
import com.megacorp.humanresources.service.helper.EmailServiceHelper;

import io.modelcontextprotocol.client.McpSyncClient;
import org.eclipse.angus.mail.imap.IMAPFolder;

import jakarta.mail.Folder;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountAdapter;
import jakarta.mail.event.MessageCountEvent;

/**
 * Service that uses IMAP IDLE to listen for new emails arriving in Gmail inbox.
 * Runs in a background thread and notifies when new emails arrive.
 */
@Service
public class EmailIdleService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailIdleService.class);
    private static final String IMAP_SERVER = "imap.gmail.com";
    private static final String POLICY_INDEX_NAME = "employee_code_of_conduct_policies";
    private static final String POLICY_GCS_PREFIX = "policies/";
    private static final long POLICY_TEXT_CACHE_TTL_MILLIS = 15 * 60 * 1000;
    private static final Set<String> POLICY_KEYWORDS = Set.of(
        "policy", "policies", "code of conduct", "dress code", "cyber security", "internet usage",
        "email usage", "social media", "cell phone", "conflict of interest", "fraternization",
        "employment of relatives", "workplace visitor", "solicitation"
    );
    private static final Set<String> EMPLOYEE_KEYWORDS = Set.of(
        "employee", "employees", "headcount", "count", "how many", "manager", "department",
        "salary", "title", "hire date", "ethnicity", "gender", "employee id", "who is"
    );
    
    @Value("${gmail_email_address}")
    private String emailAddress;
    
    @Value("${gmail_email_app_password}")
    private String emailPassword;
    
    @Autowired
    private EmailServiceHelper emailHelper;

    private final EmailService emailService;
    private final EmployeeService employeeService;
    private final RagService ragService;
    private final FileStorageService fileStorageService;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CachedPolicyText> policyTextCache = new ConcurrentHashMap<>();
    
    private volatile boolean running = false;
    private Thread idleThread;

    public EmailIdleService(
        EmailServiceHelper emailHelper,
        EmailService emailService,
        EmployeeService employeeService,
        RagService ragService,
        FileStorageService fileStorageService,
        ChatClient.Builder chatClientBuilder,
        List<McpSyncClient> mcpSyncClients,
        CallAdvisor chatClientLoggingAdvisor,
        ObjectMapper objectMapper
    ) {
        this.emailHelper = emailHelper;
        this.emailService = emailService;
        this.employeeService = employeeService;
        this.ragService = ragService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
        this.chatClient = chatClientBuilder
            .defaultAdvisors(chatClientLoggingAdvisor)
            .defaultToolCallbacks(SyncMcpToolCallbackProvider.builder().mcpClients(mcpSyncClients).build())
            .build();
    }
    
    /**
     * Starts the IMAP IDLE listener in a background thread.
     * The listener will automatically reconnect on connection failures.
     */
    public void startIdleListener() {
        logger.debug("Entering startIdleListener");
        if (running) {
            logger.warn("IDLE listener already running");
            return;
        }
        
        running = true;
        idleThread = new Thread(() -> {
            logger.info("IMAP IDLE listener thread started");
            while (running) {
                try {
                    listenForNewEmails();
                } catch (Exception e) {
                    logger.error("Error in IDLE listener: {}", e.getMessage(), e);
                    if (running) {
                        try {
                            // Wait before reconnecting
                            logger.warn("Waiting 5 seconds before reconnecting after listener error");
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            logger.info("IMAP IDLE listener thread stopped");
        }, "email-idle-listener");
        
        idleThread.setDaemon(true);
        idleThread.start();
        logger.info("Started IMAP IDLE listener for {}", emailAddress);
    }
    
    /**
     * Stops the IMAP IDLE listener gracefully.
     */
    public void stopIdleListener() {
        logger.debug("Entering stopIdleListener");
        logger.info("Stopping IMAP IDLE listener...");
        running = false;
        if (idleThread != null) {
            idleThread.interrupt();
            try {
                idleThread.join(5000); // Wait up to 5 seconds for thread to stop
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        logger.info("IMAP IDLE listener stopped");
    }
    
    /**
     * Main loop that maintains IMAP connection and listens for new emails.
     */
    private void listenForNewEmails() throws Exception {
        logger.debug("Entering listenForNewEmails");
        Session session = emailHelper.getImapSession();
        Store store = null;
        IMAPFolder inbox = null;
        
        try {
            logger.info("Connecting to Gmail IMAP server...");
            store = session.getStore("imap");
            store.connect(IMAP_SERVER, emailAddress, emailPassword);
            
            inbox = (IMAPFolder) store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            
            logger.info("Connected to INBOX. Current message count: {}", inbox.getMessageCount());
            
            // Add listener for new messages
            inbox.addMessageCountListener(new MessageCountAdapter() {
                @Override
                public void messagesAdded(MessageCountEvent ev) {
                    Message[] msgs = ev.getMessages();
                    logger.info("🆕 New messages arrived: {}", msgs.length);
                    
                    for (Message msg : msgs) {
                        try {
                            handleNewEmail(msg);
                        } catch (Exception e) {
                            logger.error("Error processing new email: {}", e.getMessage(), e);
                        }
                    }
                }
            });
            
            // Keep connection alive with IDLE
            while (running && inbox.isOpen()) {
                logger.debug("Starting IDLE (waiting for new messages)...");
                
                // IDLE for up to 25 minutes (Gmail timeout is ~29 minutes)
                // This blocks until a message arrives or timeout occurs
                inbox.idle(true);
                
                logger.debug("IDLE returned, checking for updates...");
                
                // After IDLE returns, send NOOP to keep connection alive
                if (inbox.isOpen()) {
                    inbox.doCommand(protocol -> {
                        protocol.simpleCommand("NOOP", null);
                        return null;
                    });
                }
            }
            
        } finally {
            try {
                if (inbox != null && inbox.isOpen()) {
                    inbox.close(false);
                    logger.debug("Closed inbox folder");
                }
            } catch (Exception e) {
                logger.warn("Error closing inbox", e);
            }
            
            try {
                if (store != null && store.isConnected()) {
                    store.close();
                    logger.debug("Closed IMAP store");
                }
            } catch (Exception e) {
                logger.warn("Error closing store", e);
            }
        }
    }
    
    /**
     * Handles a new email that arrived in the inbox.
     * Override or modify this method to add custom processing logic.
     * 
     * @param message the new email message
     */
    private void handleNewEmail(Message message) throws Exception {
        logger.debug("Entering handleNewEmail");
        // Convert to EmailMessage using existing helper
        EmailMessage emailMessage = emailHelper.convertToEmailMessage(message);
        
        logger.info("📧 New email received:");
        logger.info("  From: {}", emailMessage.getFrom());
        logger.info("  Subject: {}", emailMessage.getSubject());
        logger.info("  Date: {}", emailMessage.getReceivedDate());
        logger.info("  Message-ID: {}", emailMessage.getMessageId());

        if (emailMessage.getMessageId() == null || emailMessage.getMessageId().isBlank()) {
            logger.warn("Skipping email with empty Message-ID; cannot create threaded reply");
            return;
        }

        FlowType flowType = classifyFlow(emailMessage);
        if (flowType == FlowType.UNSUPPORTED) {
            logger.info("Unable to auto-respond. Message-ID={} subject={}", emailMessage.getMessageId(), emailMessage.getSubject());
            return;
        }

        String recipient = extractSenderEmail(emailMessage.getFrom());
        if (recipient == null || recipient.isBlank()) {
            logger.warn("Skipping auto-reply because sender email could not be parsed. from={}", emailMessage.getFrom());
            return;
        }

        List<String> availablePolicyFiles = flowType == FlowType.POLICY ? listAvailablePolicyPdfFiles() : List.of();
        List<String> inferredPolicyAttachments = flowType == FlowType.POLICY
            ? inferPolicyAttachments(emailMessage, availablePolicyFiles)
            : List.of();
        String policyContext = flowType == FlowType.POLICY
            ? buildPolicyContextFromGcs(inferredPolicyAttachments)
            : "";

        if (flowType == FlowType.POLICY) {
            String policyQuestion = buildPolicyQuestion(emailMessage);
            PolicyRagResponse policyRagResponse = ragService.queryPolicies(policyQuestion, 5, 0.6);

            if (policyRagResponse != null) {
                if (policyRagResponse.supportingContext() != null && !policyRagResponse.supportingContext().isBlank()) {
                    policyContext = policyRagResponse.supportingContext();
                }

                if (policyRagResponse.attachmentPaths() != null && !policyRagResponse.attachmentPaths().isEmpty()) {
                    inferredPolicyAttachments = policyRagResponse.attachmentPaths();
                }

                if (policyRagResponse.answer() != null && !policyRagResponse.answer().isBlank()) {
                    policyContext = "RAG_ANSWER_HINT:\n" + policyRagResponse.answer() + "\n\nPOLICY_CONTEXT:\n" + policyContext;
                }
            }
        }

        DraftResponse draftResponse = generateDraft(emailMessage, flowType, policyContext, inferredPolicyAttachments);
        if (draftResponse.replyBody() == null || draftResponse.replyBody().isBlank()) {
            logger.warn("Generated draft body was empty. Skipping draft creation for Message-ID={}", emailMessage.getMessageId());
            return;
        }

        List<String> storageAttachments = flowType == FlowType.POLICY
            ? resolvePolicyAttachments(draftResponse, availablePolicyFiles, inferredPolicyAttachments)
            : Collections.emptyList();

        String subject = emailMessage.getSubject() == null || emailMessage.getSubject().isBlank()
            ? "Email reply"
            : emailMessage.getSubject();

        emailService.saveDraftEmail(
            recipient,
            null,
            subject,
            draftResponse.replyBody(),
            null,
            storageAttachments,
            emailMessage.getMessageId()
        );
        logger.info("Draft reply saved for Message-ID={} with {} policy attachment(s)", emailMessage.getMessageId(), storageAttachments.size());

        emailService.markEmailAsRead(emailMessage.getMessageId());
        logger.info("Marked original email as read: {}", emailMessage.getMessageId());
    }
    
    /**
     * Returns whether the IDLE listener is currently running.
     */
    public boolean isRunning() {
        return running;
    }

    private FlowType classifyFlow(EmailMessage emailMessage) {
        FlowType aiFlow = classifyFlowWithAi(emailMessage);
        if (aiFlow != null) {
            return aiFlow;
        }

        logger.info("Falling back to keyword-based flow classification for Message-ID={}", emailMessage.getMessageId());
        return classifyFlowByKeywords(emailMessage);
    }

    private FlowType classifyFlowWithAi(EmailMessage emailMessage) {
        String classificationPrompt = """
            Classify this incoming HR email into exactly one flow:
            - POLICY: questions about company policies, code of conduct, or rule documents.
            - EMPLOYEE: questions about employee counts, employee records, or individual employee details.
            - UNSUPPORTED: everything else.

            Return ONLY valid JSON with this shape:
            {
              "flow": "POLICY|EMPLOYEE|UNSUPPORTED",
              "reason": "one short sentence"
            }

            Email:
            Subject: %s
            Body:
            %s
            """.formatted(
                emailMessage.getSubject() == null ? "" : emailMessage.getSubject(),
                emailMessage.getBody() == null ? "" : emailMessage.getBody()
            );

        try {
            String rawResponse = chatClient.prompt()
                .system("You are a strict email intent classifier. Output JSON only.")
                .user(classificationPrompt)
                .call()
                .content();

            if (rawResponse == null || rawResponse.isBlank()) {
                return null;
            }

            String cleaned = rawResponse
                .replace("```json", "")
                .replace("```", "")
                .trim();

            FlowClassificationDto dto = objectMapper.readValue(cleaned, FlowClassificationDto.class);
            if (dto.flow == null || dto.flow.isBlank()) {
                return null;
            }

            FlowType flow = FlowType.fromString(dto.flow);
            logger.info("AI classified flow={} for Message-ID={} reason={}", flow, emailMessage.getMessageId(), dto.reason);
            return flow;
        } catch (Exception e) {
            logger.warn("AI flow classification failed for Message-ID={}", emailMessage.getMessageId(), e);
            return null;
        }
    }

    private FlowType classifyFlowByKeywords(EmailMessage emailMessage) {
        String content = ((emailMessage.getSubject() == null ? "" : emailMessage.getSubject()) + " " +
            (emailMessage.getBody() == null ? "" : emailMessage.getBody())).toLowerCase(Locale.ROOT);

        for (String policyKeyword : POLICY_KEYWORDS) {
            if (content.contains(policyKeyword)) {
                return FlowType.POLICY;
            }
        }

        for (String employeeKeyword : EMPLOYEE_KEYWORDS) {
            if (content.contains(employeeKeyword)) {
                return FlowType.EMPLOYEE;
            }
        }

        return FlowType.UNSUPPORTED;
    }

    private DraftResponse generateDraft(EmailMessage emailMessage, FlowType flowType, String policyContext,
            List<String> inferredPolicyAttachments) {
        String userPrompt = buildUserPrompt(emailMessage, flowType, policyContext, inferredPolicyAttachments);

        String rawResponse;
        if (flowType == FlowType.EMPLOYEE) {
            rawResponse = chatClient.prompt()
                .tools(employeeService)
                .system(buildSystemPrompt(flowType))
                .user(userPrompt)
                .call()
                .content();
        } else {
            rawResponse = chatClient.prompt()
                .tools(employeeService)
                .system(buildSystemPrompt(flowType))
                .user(userPrompt)
                .call()
                .content();
        }
        logger.debug("Generated draft rawResponse: {}", rawResponse);
        return parseDraftResponse(rawResponse);
    }

    private String buildSystemPrompt(FlowType flowType) {
        if (flowType == FlowType.POLICY) {
            return ("""
                You are drafting a professional HR email reply.
                Requirements:
                1) This is a company policy question.
                2) Use MCP tools to search Elasticsearch index '%s'.
                                3) Use matching policy content field(s) to answer.
                                4) If POLICY_CONTEXT is provided, prioritize that content and quote the policy's guidance in plain language.
                                5) Ensure response addresses the user's exact question; for personal-use questions, include the policy's personal-use guidance.
                                6) Include likely policy PDF attachment paths under 'policies/' in storageAttachments when relevant.
                                7) Keep answer concise, correct, and business-professional.
                                8) Include references to relevant policy names in the reply body.
                                9) Return ONLY valid JSON with this exact shape:
                   {
                     "replyBody": "...",
                     "referencedPolicies": ["Policy Name"],
                                         "storageAttachments": ["policies/Business Dress Code Policy.pdf"]
                   }
                Do not include markdown fences.
                """).formatted(POLICY_INDEX_NAME);
        }

        return """
            You are drafting a professional HR email reply.
            Requirements:
            1) This question is about employee counts or specific employee data.
            2) Use employeeService tools to gather accurate data before answering.
            3) Keep the response concise and factual; do not invent values.
            4) Return ONLY valid JSON with this exact shape:
               {
                 "replyBody": "...",
                 "referencedPolicies": [],
                 "storageAttachments": []
               }
            Do not include markdown fences.
            """;
    }

    private String buildUserPrompt(EmailMessage emailMessage, FlowType flowType, String policyContext,
            List<String> inferredPolicyAttachments) {
        String base = """
            Incoming email details:
            - From: %s
            - Subject: %s
            - Message-ID: %s
            - Body:
            %s

            Prepare the draft reply for this %s request.
            """.formatted(
                emailMessage.getFrom(),
                emailMessage.getSubject(),
                emailMessage.getMessageId(),
                emailMessage.getBody(),
                flowType == FlowType.POLICY ? "policy" : "employee-data"
            );

        if (flowType != FlowType.POLICY) {
            return base;
        }

        String suggestedAttachments = inferredPolicyAttachments.isEmpty()
            ? "None"
            : String.join(", ", inferredPolicyAttachments);

        String resolvedPolicyContext = (policyContext == null || policyContext.isBlank())
            ? "No GCS policy context available."
            : policyContext;

        return base + """

            Suggested policy attachments:
            %s

            POLICY_CONTEXT:
            %s
                """.formatted(suggestedAttachments, resolvedPolicyContext);
    }

    private DraftResponse parseDraftResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return new DraftResponse("", List.of(), List.of());
        }

        String cleaned = rawResponse
            .replace("```json", "")
            .replace("```", "")
            .trim();

        try {
            DraftResponseDto dto = objectMapper.readValue(cleaned, DraftResponseDto.class);
            logger.debug("Parsed draft response: {}", dto);
            return new DraftResponse(
                dto.replyBody == null ? "" : dto.replyBody,
                dto.referencedPolicies == null ? List.of() : dto.referencedPolicies,
                dto.storageAttachments == null ? List.of() : dto.storageAttachments
            );
        } catch (Exception e) {
            logger.warn("Failed to parse AI JSON response. Falling back to plain text body", e);
            return new DraftResponse(cleaned, List.of(), List.of());
        }
    }

    private List<String> resolvePolicyAttachments(DraftResponse draftResponse, List<String> availablePolicyFiles,
            List<String> inferredPolicyAttachments) {
        if (availablePolicyFiles == null || availablePolicyFiles.isEmpty()) {
            return List.of();
        }

        List<String> availablePdfFiles = availablePolicyFiles.stream()
            .filter(fileName -> fileName != null && fileName.startsWith(POLICY_GCS_PREFIX) && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            .toList();

        if (availablePdfFiles.isEmpty()) {
            return List.of();
        }

        Set<String> resolved = new LinkedHashSet<>();

        for (String attachment : draftResponse.storageAttachments()) {
            String normalized = normalizePolicyAttachmentPath(attachment);
            if (normalized != null && availablePdfFiles.contains(normalized)) {
                resolved.add(normalized);
            }
        }

        if (resolved.isEmpty()) {
            for (String title : draftResponse.referencedPolicies()) {
                String expectedCurrent = POLICY_GCS_PREFIX + title + ".pdf";
                String expectedLegacy = POLICY_GCS_PREFIX + "policies_" + title + ".pdf";

                if (availablePdfFiles.contains(expectedCurrent)) {
                    resolved.add(expectedCurrent);
                } else if (availablePdfFiles.contains(expectedLegacy)) {
                    resolved.add(expectedLegacy);
                } else {
                    String normalizedTitle = title.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
                    for (String availableFile : availablePdfFiles) {
                        String availableName = availableFile.substring(POLICY_GCS_PREFIX.length()).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
                        if (availableName.contains(normalizedTitle)) {
                            resolved.add(availableFile);
                        }
                    }
                }
            }
        }

        if (resolved.isEmpty() && inferredPolicyAttachments != null && !inferredPolicyAttachments.isEmpty()) {
            for (String inferredAttachment : inferredPolicyAttachments) {
                if (availablePdfFiles.contains(inferredAttachment)) {
                    resolved.add(inferredAttachment);
                }
            }
        }

        return new ArrayList<>(resolved);
    }

    private List<String> listAvailablePolicyPdfFiles() {
        try {
            List<String> availablePolicyFiles = fileStorageService.listFiles(POLICY_GCS_PREFIX);
            return availablePolicyFiles.stream()
                .filter(fileName -> fileName != null
                    && fileName.startsWith(POLICY_GCS_PREFIX)
                    && fileName.toLowerCase(Locale.ROOT).endsWith(".pdf"))
                .toList();
        } catch (Exception e) {
            logger.warn("Unable to list policy files from GCS path {}", POLICY_GCS_PREFIX, e);
            return List.of();
        }
    }

    private List<String> inferPolicyAttachments(EmailMessage emailMessage, List<String> availablePolicyFiles) {
        if (availablePolicyFiles == null || availablePolicyFiles.isEmpty()) {
            return List.of();
        }

        String content = ((emailMessage.getSubject() == null ? "" : emailMessage.getSubject()) + " " +
            (emailMessage.getBody() == null ? "" : emailMessage.getBody())).toLowerCase(Locale.ROOT);

        Set<String> inferred = new LinkedHashSet<>();
        if (content.contains("email") || content.contains("mail")) {
            availablePolicyFiles.stream()
                .filter(fileName -> fileName.toLowerCase(Locale.ROOT).contains("email usage policy"))
                .findFirst()
                .ifPresent(inferred::add);
        }

        if (content.contains("internet")) {
            availablePolicyFiles.stream()
                .filter(fileName -> fileName.toLowerCase(Locale.ROOT).contains("internet usage policy"))
                .findFirst()
                .ifPresent(inferred::add);
        }

        if (content.contains("dress") || content.contains("clothing") || content.contains("attire")) {
            availablePolicyFiles.stream()
                .filter(fileName -> fileName.toLowerCase(Locale.ROOT).contains("business dress code policy"))
                .findFirst()
                .ifPresent(inferred::add);
        }

        if (inferred.size() < 2) {
            List<ScoredPolicy> scoredPolicies = availablePolicyFiles.stream()
                .map(fileName -> new ScoredPolicy(fileName, scorePolicyAgainstQuestion(fileName, content)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(ScoredPolicy::score).reversed())
                .toList();

            for (ScoredPolicy scoredPolicy : scoredPolicies) {
                inferred.add(scoredPolicy.path());
                if (inferred.size() >= 3) {
                    break;
                }
            }
        }

        return new ArrayList<>(inferred);
    }

    private int scorePolicyAgainstQuestion(String policyPath, String lowerQuestion) {
        String title = extractPolicyTitleFromPath(policyPath).toLowerCase(Locale.ROOT);
        String[] tokens = title.split("\\s+");
        int score = 0;

        for (String token : tokens) {
            if (token.length() < 3 || "policy".equals(token) || "company".equals(token)) {
                continue;
            }
            if (lowerQuestion.contains(token)) {
                score += 2;
            }
        }

        if (lowerQuestion.contains("personal") && title.contains("email usage")) {
            score += 5;
        }

        return score;
    }

    private String buildPolicyContextFromGcs(List<String> inferredPolicyAttachments) {
        if (inferredPolicyAttachments == null || inferredPolicyAttachments.isEmpty()) {
            return "";
        }

        List<String> snippets = new ArrayList<>();
        for (String attachment : inferredPolicyAttachments) {
            String text = readPolicyPdfTextFromGcs(attachment);
            if (text != null && !text.isBlank()) {
                String title = extractPolicyTitleFromPath(attachment);
                String normalized = text.replaceAll("\\s+", " ").trim();
                if (normalized.length() > 2200) {
                    normalized = normalized.substring(0, 2200);
                }
                snippets.add("Policy: " + title + "\n" + normalized);
            }
        }

        return snippets.stream().collect(Collectors.joining("\n\n"));
    }

    private String readPolicyPdfTextFromGcs(String gcsPolicyPath) {
        CachedPolicyText cached = policyTextCache.get(gcsPolicyPath);
        long now = System.currentTimeMillis();
        if (cached != null && (now - cached.cachedAtMillis()) < POLICY_TEXT_CACHE_TTL_MILLIS) {
            return cached.text();
        }

        try {
            byte[] fileBytes = fileStorageService.retrieveFile(gcsPolicyPath);
            if (fileBytes == null || fileBytes.length == 0) {
                logger.warn("Policy file not found or empty in GCS: {}", gcsPolicyPath);
                return "";
            }

            PagePdfDocumentReader reader = new PagePdfDocumentReader(new ByteArrayResource(fileBytes));
            List<Document> docs = reader.read();
            String text = docs.stream().map(Document::getText).collect(Collectors.joining("\n"));
            policyTextCache.put(gcsPolicyPath, new CachedPolicyText(text, now));
            return text;
        } catch (Exception e) {
            logger.warn("Failed reading policy pdf from GCS for {}", gcsPolicyPath, e);
            return "";
        }
    }

    private String extractPolicyTitleFromPath(String policyPath) {
        if (policyPath == null || policyPath.isBlank()) {
            return "";
        }
        String name = policyPath.startsWith(POLICY_GCS_PREFIX)
            ? policyPath.substring(POLICY_GCS_PREFIX.length())
            : policyPath;
        if (name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            name = name.substring(0, name.length() - 4);
        }
        if (name.startsWith("policies_")) {
            name = name.substring("policies_".length());
        }
        return name;
    }

    private String normalizePolicyAttachmentPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }

        String normalized = rawPath.trim();
        if (normalized.startsWith(POLICY_GCS_PREFIX) && normalized.endsWith(".pdf")) {
            return normalized;
        }

        if (normalized.endsWith(".pdf")) {
            int slashIndex = normalized.lastIndexOf('/');
            String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
            return POLICY_GCS_PREFIX + fileName;
        }

        return null;
    }

    private String extractSenderEmail(String fromField) {
        if (fromField == null || fromField.isBlank()) {
            return null;
        }

        try {
            InternetAddress[] parsed = InternetAddress.parse(fromField);
            if (parsed.length > 0 && parsed[0].getAddress() != null) {
                return parsed[0].getAddress();
            }
        } catch (Exception e) {
            logger.warn("Could not parse sender email from fromField={}", fromField, e);
        }
        return fromField;
    }

    private String buildPolicyQuestion(EmailMessage emailMessage) {
        String subject = emailMessage.getSubject() == null ? "" : emailMessage.getSubject();
        String body = emailMessage.getBody() == null ? "" : emailMessage.getBody();
        return (subject + "\n\n" + body).trim();
    }

    private record DraftResponse(String replyBody, List<String> referencedPolicies, List<String> storageAttachments) {
    }

    private static class DraftResponseDto {
        public String replyBody;

        @Override
        public String toString() {
            return "DraftResponseDto{" +
                "replyBody='" + replyBody + '\'' +
                ", referencedPolicies=" + referencedPolicies +
                ", storageAttachments=" + storageAttachments +
                '}';
        }
        public List<String> referencedPolicies;
        public List<String> storageAttachments;
    }

    private static class FlowClassificationDto {
        public String flow;
        public String reason;
    }

    private record ScoredPolicy(String path, int score) {}

    private record CachedPolicyText(String text, long cachedAtMillis) {}

    private enum FlowType {
        POLICY,
        EMPLOYEE,
        UNSUPPORTED;

        private static FlowType fromString(String value) {
            try {
                return FlowType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (Exception e) {
                return UNSUPPORTED;
            }
        }
    }
}
