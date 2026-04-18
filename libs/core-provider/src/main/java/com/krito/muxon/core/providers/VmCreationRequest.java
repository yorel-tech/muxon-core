package com.krito.muxon.core.providers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request for VM creation with provider context
 */
public record VmCreationRequest(
        UUID vmId,
        String spec,
        ProviderContext providerContext,
        Map<String, String> metadata,
        String correlationId,
        String sourceImagePath,
        List<IsoAttachment> isoAttachments
) {
    public VmCreationRequest {
        isoAttachments = isoAttachments == null ? List.of() : List.copyOf(isoAttachments);
    }

    public static VmCreationRequestBuilder builder() {
        return new VmCreationRequestBuilder();
    }

    public static class VmCreationRequestBuilder {
        private UUID vmId;
        private String spec;
        private ProviderContext providerContext;
        private Map<String, String> metadata;
        private String correlationId;
        private String sourceImagePath;
        private List<IsoAttachment> isoAttachments = Collections.emptyList();

        public VmCreationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        public VmCreationRequestBuilder spec(String spec) {
            this.spec = spec;
            return this;
        }

        public VmCreationRequestBuilder providerContext(ProviderContext providerContext) {
            this.providerContext = providerContext;
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

        public VmCreationRequestBuilder sourceImagePath(String sourceImagePath) {
            this.sourceImagePath = sourceImagePath;
            return this;
        }

        public VmCreationRequestBuilder isoAttachments(List<IsoAttachment> isoAttachments) {
            this.isoAttachments = isoAttachments == null ? Collections.emptyList() : isoAttachments;
            return this;
        }

        public VmCreationRequest build() {
            return new VmCreationRequest(vmId, spec, providerContext, metadata, correlationId, sourceImagePath, isoAttachments);
        }
    }
}
