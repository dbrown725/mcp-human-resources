package com.megacorp.humanresources.exceptions;

public class ResourceNotFoundException extends ApplicationException {

    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super("RESOURCE_NOT_FOUND", resourceName + " not found for id: " + resourceId);
    }
}
