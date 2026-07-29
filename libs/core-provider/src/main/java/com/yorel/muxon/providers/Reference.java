package com.yorel.muxon.providers;

import java.util.Objects;
import java.util.UUID;

/**
 * Reference to a resource with both name and external ID
 * External ID is used for API operations, name for display/logging
 */
public record Reference(
    String name,           // Human-readable name for display/logging
    String externalId,     // External ID used by provider APIs (VMID, node ID, etc.)
    UUID internalId       // Internal database UUID (for Libvirt, null for external providers)
) {
    
    public Reference(String name, String externalId) {
        this(name, externalId, null);
    }
    
    public Reference(String name, String externalId, UUID internalId) {
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.externalId = Objects.requireNonNull(externalId, "externalId cannot be null");
        this.internalId = internalId;
    }
    
    /**
     * Create reference for external providers (no internal ID)
     */
    public static Reference external(String name, String externalId) {
        return new Reference(name, externalId);
    }
    
    /**
     * Create reference for internal providers (with database UUID)
     */
    public static Reference internal(String name, String externalId, UUID internalId) {
        return new Reference(name, externalId, internalId);
    }
    
    /**
     * Check if this is an external provider reference
     */
    public boolean isExternal() {
        return internalId == null;
    }
    
    /**
     * Check if this is an internal provider reference
     */
    public boolean isInternal() {
        return internalId != null;
    }
}
