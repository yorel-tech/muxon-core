package com.krito.muxon.providers;

import java.util.Map;

/**
 * Provider-specific placement information
 */
public record ProviderPlacementInfo(
    Reference targetResource,  // Reference with name + external ID
    Map<String, String> preferences,  // storage pool, network bridge, etc.
    Map<String, String> constraints   // affinity rules, resource limits, etc.
) {
    public static ProviderPlacementInfoBuilder builder() {
        return new ProviderPlacementInfoBuilder();
    }
    
    public static class ProviderPlacementInfoBuilder {
        private Reference targetResource;
        private Map<String, String> preferences;
        private Map<String, String> constraints;
        
        public ProviderPlacementInfoBuilder targetResource(Reference targetResource) {
            this.targetResource = targetResource;
            return this;
        }
        
        public ProviderPlacementInfoBuilder preferences(Map<String, String> preferences) {
            this.preferences = preferences;
            return this;
        }
        
        public ProviderPlacementInfoBuilder constraints(Map<String, String> constraints) {
            this.constraints = constraints;
            return this;
        }
        
        public ProviderPlacementInfo build() {
            return new ProviderPlacementInfo(targetResource, preferences, constraints);
        }
    }
}
