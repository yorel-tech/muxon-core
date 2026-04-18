package com.krito.muxon.core.providers;

import java.util.UUID;

/**
 * Request for VM deletion
 */
public record VmDeletionRequest(
        UUID vmId,
        String correlationId
) {
    public static VmDeletionRequestBuilder builder() {
        return new VmDeletionRequestBuilder();
    }

    public static class VmDeletionRequestBuilder {
        private UUID vmId;
        private String correlationId;

        public VmDeletionRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        public VmDeletionRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public VmDeletionRequest build() {
            return new VmDeletionRequest(vmId, correlationId);
        }
    }
}
