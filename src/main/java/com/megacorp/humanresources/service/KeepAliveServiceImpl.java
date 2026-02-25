package com.megacorp.humanresources.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class KeepAliveServiceImpl implements KeepAliveService {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveServiceImpl.class);

    @Tool(
		name = "keep_alive",
		description = "Returns a keep alive response."
	)
    public String keepAlive() {
        logger.debug("Entering keepAlive");
        // This method can be used to perform a simple operation to keep the service alive.
        // For example, returning a simple message or performing a lightweight operation.
        logger.info("Keep-alive request processed successfully");
        return "KEEP_ALIVE_RESPONSE";
    }
    
}
