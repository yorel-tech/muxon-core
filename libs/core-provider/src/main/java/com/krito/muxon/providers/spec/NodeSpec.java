package com.krito.muxon.providers.spec;

import com.krito.muxon.api.model.Node;
import com.krito.muxon.api.model.Resources;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Node specification containing node configuration data.
 * This is a value object that replaces NodeEntity dependency in providers.
 */
public record NodeSpec(
        UUID id,
        String name,
        String externalId,
        String endpoint,
        String role,
        Integer cpuTotal,
        Integer memMb,
        Node.StatusEnum status,
        Instant lastSeenAt,
        Map<String, String> credentials,
        List<String> ipAddresses,
        List<String> capabilities,
        Resources resources
) {
    
    public static NodeSpecBuilder builder() {
        return new NodeSpecBuilder();
    }
    
    public static class NodeSpecBuilder {
        private UUID id;
        private String name;
        private String externalId;
        private String endpoint;
        private String role;
        private Integer cpuTotal;
        private Integer memMb;
        private Node.StatusEnum status;
        private Instant lastSeenAt;
        private Map<String, String> credentials;
        private List<String> ipAddresses;
        private List<String> capabilities;
        private Resources resources;
        
        public NodeSpecBuilder id(UUID id) {
            this.id = id;
            return this;
        }
        
        public NodeSpecBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public NodeSpecBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }
        
        public NodeSpecBuilder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }
        
        public NodeSpecBuilder role(String role) {
            this.role = role;
            return this;
        }
        
        public NodeSpecBuilder cpuTotal(Integer cpuTotal) {
            this.cpuTotal = cpuTotal;
            return this;
        }
        
        public NodeSpecBuilder memMb(Integer memMb) {
            this.memMb = memMb;
            return this;
        }
        
        public NodeSpecBuilder status(Node.StatusEnum status) {
            this.status = status;
            return this;
        }
        
        public NodeSpecBuilder lastSeenAt(Instant lastSeenAt) {
            this.lastSeenAt = lastSeenAt;
            return this;
        }
        
        public NodeSpecBuilder credentials(Map<String, String> credentials) {
            this.credentials = credentials;
            return this;
        }
        
        public NodeSpecBuilder ipAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
            return this;
        }
        
        public NodeSpecBuilder capabilities(List<String> capabilities) {
            this.capabilities = capabilities;
            return this;
        }
        
        public NodeSpecBuilder resources(Resources resources) {
            this.resources = resources;
            return this;
        }
        
        public NodeSpec build() {
            return new NodeSpec(
                    id,
                    name,
                    externalId,
                    endpoint,
                    role,
                    cpuTotal,
                    memMb,
                    status,
                    lastSeenAt,
                    credentials,
                    ipAddresses,
                    capabilities,
                    resources
            );
        }
    }
}
