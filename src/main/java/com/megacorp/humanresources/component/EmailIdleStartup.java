package com.megacorp.humanresources.component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.megacorp.humanresources.service.EmailIdleService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Component that manages the lifecycle of the Email IDLE listener.
 * Automatically starts the listener when the application starts
 * and stops it gracefully on shutdown.
 */
@Component
public class EmailIdleStartup {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailIdleStartup.class);
    
    @Autowired
    private EmailIdleService emailIdleService;
    
    /**
     * Starts the IMAP IDLE listener when the application starts.
     */
    @PostConstruct
    public void init() {
        logger.debug("Entering init");
        logger.info("Initializing Email IDLE listener...");
        try {
            emailIdleService.startIdleListener();
            logger.info("Email IDLE listener initialized successfully");
        } catch (Exception e) {
            logger.warn("Failed to start Email IDLE listener; application will continue without listener", e);
            // Don't fail application startup if email listener fails
        }
    }
    
    /**
     * Stops the IMAP IDLE listener gracefully when the application shuts down.
     */
    @PreDestroy
    public void cleanup() {
        logger.debug("Entering cleanup");
        logger.info("Shutting down Email IDLE listener...");
        try {
            emailIdleService.stopIdleListener();
            logger.info("Email IDLE listener shut down successfully");
        } catch (Exception e) {
            logger.warn("Error during Email IDLE listener shutdown", e);
        }
    }
}
