package com.yorel.muxon.providers;

import com.yorel.muxon.api.model.ProviderType;
import java.util.Map;
import java.util.Optional;

/**
 * Provider-specific context containing all necessary data for VM operations.
 * Each provider type implements this interface to provide its specific context.
 */
public interface ProviderContext {
    
    /**
     * Get the provider type
     */
    ProviderType getProviderType();
    
    /**
     * Get provider connection information (endpoint, credentials)
     */
    ProviderConnectionInfo getConnectionInfo();
    
    /**
     * Get placement information specific to this provider
     */
    Optional<ProviderPlacementInfo> getPlacementInfo();
    
    /**
     * Get provider-specific metadata
     */
    Map<String, Object> getMetadata();
    
    /**
     * Get target resource reference (name + external ID)
     */
    Optional<Reference> getTargetResource();
}
