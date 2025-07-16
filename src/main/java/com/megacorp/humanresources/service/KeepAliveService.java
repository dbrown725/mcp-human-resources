package com.megacorp.humanresources.service;

public interface KeepAliveService {
    /**
     * Keeps the service alive by performing a simple operation.
     * This can be used to ensure that the service is responsive and operational.
     */
    String keepAlive();
}
