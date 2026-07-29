package com.yorel.muxon.providers;

import java.util.Map;

/**
 * Connection information for a provider
 */
public record ProviderConnectionInfo(
    String endpoint,
    Map<String, String> credentials,
    Map<String, Object> connectionConfig
) {
    public static ProviderConnectionInfoBuilder builder() {
        return new ProviderConnectionInfoBuilder();
    }
    
    public static class ProviderConnectionInfoBuilder {
        private String endpoint;
        private Map<String, String> credentials;
        private Map<String, Object> connectionConfig;
        
        public ProviderConnectionInfoBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public ProviderConnectionInfoBuilder credentials(Map<String, String> credentials) {
            this.credentials = credentials;
            return this;
        }
        
        public ProviderConnectionInfoBuilder connectionConfig(Map<String, Object> connectionConfig) {
            this.connectionConfig = connectionConfig;
            return this;
        }
        
        public ProviderConnectionInfo build() {
            return new ProviderConnectionInfo(endpoint, credentials, connectionConfig);
        }
    }
}
