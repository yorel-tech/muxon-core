package com.krito.muxon.core.providers;

import java.util.Map;
import java.util.UUID;

/**
 * Request for VM operation (start, stop, restart, suspend, resume)
 */
public record VmOperationRequest(
        UUID vmId,
        String externalVmId,
        Map<String, String> parameters,
        String correlationId
) {
    public static VmOperationRequestBuilder builder() {
        return new VmOperationRequestBuilder();
    }

    public static class VmOperationRequestBuilder {
        private UUID vmId;
        private String externalVmId;
        private Map<String, String> parameters;
        private String correlationId;

        public VmOperationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        public VmOperationRequestBuilder externalVmId(String externalVmId) {
            this.externalVmId = externalVmId;
            return this;
        }

        public VmOperationRequestBuilder parameters(Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public VmOperationRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public VmOperationRequest build() {
            return new VmOperationRequest(vmId, externalVmId, parameters, correlationId);
        }
    }
}
