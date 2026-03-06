package com.megacorp.humanresources.service;

import com.megacorp.humanresources.model.PolicyRagResponse;

public interface RagService {

    // void ingest(Resource path);

    // String advisedRag(String question);

    // String directRag(String question);

    int ingestPoliciesFromGcs(String prefix);

    PolicyRagResponse queryPolicies(String question, Integer topK, Double similarityThreshold);
}
