package com.megacorp.humanresources.service;

import org.springframework.core.io.Resource;

public interface RagService {

    void ingest(Resource path);

    String advisedRag(String question);

    String directRag(String question);
}
