package com.onetattva.infron.core.web.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a requested resource is not found.
 */
public class ResourceNotFoundException extends RuntimeException {
    
    private final UUID resourceId;
    private final String resourceType;
    
    public ResourceNotFoundException(UUID resourceId, String resourceType) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
    
    public ResourceNotFoundException(String resourceType, String identifier, String value) {
        super(String.format("%s not found with %s: %s", resourceType, identifier, value));
        this.resourceId = null;
        this.resourceType = resourceType;
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceId = null;
        this.resourceType = null;
    }
    
    public UUID getResourceId() {
        return resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
}
