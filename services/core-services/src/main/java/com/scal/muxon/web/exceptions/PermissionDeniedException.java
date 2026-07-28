package com.scal.muxon.web.exceptions;

import java.util.UUID;

/**
 * Exception thrown when a user lacks permission to perform an action on a resource.
 */
public class PermissionDeniedException extends RuntimeException {
    
    private final String permission;
    private final UUID resourceId;
    private final String resourceType;
    
    public PermissionDeniedException(String permission, UUID resourceId, String resourceType) {
        super(String.format("Permission denied: %s on %s with id %s", permission, resourceType, resourceId));
        this.permission = permission;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
    }
    
    public PermissionDeniedException(String permission, String resourceType) {
        super(String.format("Permission denied: %s on %s", permission, resourceType));
        this.permission = permission;
        this.resourceId = null;
        this.resourceType = resourceType;
    }
    
    public PermissionDeniedException(String message) {
        super(message);
        this.permission = null;
        this.resourceId = null;
        this.resourceType = null;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public UUID getResourceId() {
        return resourceId;
    }
    
    public String getResourceType() {
        return resourceType;
    }
}
