package com.onetattva.infron.core.providers;

import java.util.Map;
import java.util.UUID;

/**
 * Request for VM creation
 */
public record VmCreationRequest(
        UUID vmId,
        String spec,
        PlacementHints placement,
        Map<String, String> metadata,
        String correlationId
) {
    public static VmCreationRequestBuilder builder() {
        return new VmCreationRequestBuilder();
    }

    public static class VmCreationRequestBuilder {
        private UUID vmId;
        private String spec;
        private PlacementHints placement;
        private Map<String, String> metadata;
        private String correlationId;

        public VmCreationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        public VmCreationRequestBuilder spec(String spec) {
            this.spec = spec;
            return this;
        }

        public VmCreationRequestBuilder placement(PlacementHints placement) {
            this.placement = placement;
            return this;
        }

        public VmCreationRequestBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public VmCreationRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public VmCreationRequest build() {
            return new VmCreationRequest(vmId, spec, placement, metadata, correlationId);
        }
    }
}
