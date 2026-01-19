package com.onetattva.infron.core.providers;

import com.onetattva.infron.db.enums.VmPowerState;
import com.onetattva.infron.db.enums.VmStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request for VM creation
 */
record VmCreationRequest(
        UUID vmId,
        String spec,
        PlacementHints placement,
        Map<String, String> metadata,
        String correlationId
) {
    static VmCreationRequestBuilder builder() {
        return new VmCreationRequestBuilder();
    }

    static class VmCreationRequestBuilder {
        private UUID vmId;
        private String spec;
        private PlacementHints placement;
        private Map<String, String> metadata;
        private String correlationId;

        VmCreationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        VmCreationRequestBuilder spec(String spec) {
            this.spec = spec;
            return this;
        }

        VmCreationRequestBuilder placement(PlacementHints placement) {
            this.placement = placement;
            return this;
        }

        VmCreationRequestBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        VmCreationRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        VmCreationRequest build() {
            return new VmCreationRequest(vmId, spec, placement, metadata, correlationId);
        }
    }
}

/**
 * Request for VM deletion
 */
record VmDeletionRequest(
        UUID vmId,
        String correlationId
) {
    static VmDeletionRequestBuilder builder() {
        return new VmDeletionRequestBuilder();
    }

    static class VmDeletionRequestBuilder {
        private UUID vmId;
        private String correlationId;

        VmDeletionRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        VmDeletionRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        VmDeletionRequest build() {
            return new VmDeletionRequest(vmId, correlationId);
        }
    }
}

/**
 * Request for VM operation (start, stop, restart, suspend, resume)
 */
record VmOperationRequest(
        UUID vmId,
        String externalVmId,
        Map<String, String> parameters,
        String correlationId
) {
    static VmOperationRequestBuilder builder() {
        return new VmOperationRequestBuilder();
    }

    static class VmOperationRequestBuilder {
        private UUID vmId;
        private String externalVmId;
        private Map<String, String> parameters;
        private String correlationId;

        VmOperationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        VmOperationRequestBuilder externalVmId(String externalVmId) {
            this.externalVmId = externalVmId;
            return this;
        }

        VmOperationRequestBuilder parameters(Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        VmOperationRequestBuilder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        VmOperationRequest build() {
            return new VmOperationRequest(vmId, externalVmId, parameters, correlationId);
        }
    }
}

/**
 * Request for VM list
 */
record VmListRequest(
        UUID tenantDatacenterGrantId,
        String statusFilter,
        List<String> tags,
        Integer limit
) {
    static VmListRequestBuilder builder() {
        return new VmListRequestBuilder();
    }

    static class VmListRequestBuilder {
        private UUID tenantDatacenterGrantId;
        private String statusFilter;
        private List<String> tags;
        private Integer limit;

        VmListRequestBuilder tenantDatacenterGrantId(UUID tenantDatacenterGrantId) {
            this.tenantDatacenterGrantId = tenantDatacenterGrantId;
            return this;
        }

        VmListRequestBuilder statusFilter(String statusFilter) {
            this.statusFilter = statusFilter;
            return this;
        }

        VmListRequestBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        VmListRequestBuilder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        VmListRequest build() {
            return new VmListRequest(tenantDatacenterGrantId, statusFilter, tags, limit);
        }
    }
}

/**
 * Result of VM creation operation
 */
record VmCreationResult(
        ResultType resultType,
        String externalVmId,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    static VmCreationResult success(String externalVmId, VmInfo vmInfo) {
        return new VmCreationResult(ResultType.SUCCESS, externalVmId, vmInfo, null, null);
    }

    static VmCreationResult failure(String message) {
        return new VmCreationResult(ResultType.FAILED, null, null, message, null);
    }

    static VmCreationResult failure(ProviderError error) {
        return new VmCreationResult(ResultType.FAILED, null, null, null, error);
    }
}

/**
 * Result of VM deletion operation
 */
record VmDeletionResult(
        ResultType resultType,
        String message,
        ProviderError error
) {
    enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    static VmDeletionResult success() {
        return new VmDeletionResult(ResultType.SUCCESS, null, null);
    }

    static VmDeletionResult failure(String message) {
        return new VmDeletionResult(ResultType.FAILED, message, null);
    }

    static VmDeletionResult failure(ProviderError error) {
        return new VmDeletionResult(ResultType.FAILED, null, error);
    }
}

/**
 * Result of VM operation (start, stop, restart, suspend, resume)
 */
record VmOperationResult(
        ResultType resultType,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    enum ResultType {
        SUCCESS, FAILED, TIMEOUT, NOT_FOUND
    }

    static VmOperationResult success(VmInfo vmInfo) {
        return new VmOperationResult(ResultType.SUCCESS, vmInfo, null, null);
    }

    static VmOperationResult failure(String message) {
        return new VmOperationResult(ResultType.FAILED, null, message, null);
    }

    static VmOperationResult failure(ProviderError error) {
        return new VmOperationResult(ResultType.FAILED, null, null, error);
    }

    static VmOperationResult notFound() {
        return new VmOperationResult(ResultType.NOT_FOUND, null, "VM not found", null);
    }
}

/**
 * VM information
 */
record VmInfo(
        String externalVmId,
        VmStatus status,
        VmPowerState powerState,
        List<String> ipAddresses,
        String hostname,
        String resourceUsage,
        Map<String, String> metadata,
        Instant lastUpdated
) {
}

/**
 * Provider capabilities
 */
record ProviderCapabilities(
        List<String> supportedCpuTypes,
        List<String> supportedStorageClasses,
        List<String> supportedNetworkTypes,
        List<String> supportedOsTypes,
        ResourceLimits resourceLimits,
        Map<String, Object> features
) {
    static ProviderCapabilitiesBuilder builder() {
        return new ProviderCapabilitiesBuilder();
    }

    static class ProviderCapabilitiesBuilder {
        private List<String> supportedCpuTypes;
        private List<String> supportedStorageClasses;
        private List<String> supportedNetworkTypes;
        private List<String> supportedOsTypes;
        private ResourceLimits resourceLimits;
        private Map<String, Object> features;

        ProviderCapabilitiesBuilder supportedCpuTypes(List<String> supportedCpuTypes) {
            this.supportedCpuTypes = supportedCpuTypes;
            return this;
        }

        ProviderCapabilitiesBuilder supportedStorageClasses(List<String> supportedStorageClasses) {
            this.supportedStorageClasses = supportedStorageClasses;
            return this;
        }

        ProviderCapabilitiesBuilder supportedNetworkTypes(List<String> supportedNetworkTypes) {
            this.supportedNetworkTypes = supportedNetworkTypes;
            return this;
        }

        ProviderCapabilitiesBuilder supportedOsTypes(List<String> supportedOsTypes) {
            this.supportedOsTypes = supportedOsTypes;
            return this;
        }

        ProviderCapabilitiesBuilder resourceLimits(ResourceLimits resourceLimits) {
            this.resourceLimits = resourceLimits;
            return this;
        }

        ProviderCapabilitiesBuilder features(Map<String, Object> features) {
            this.features = features;
            return this;
        }

        ProviderCapabilities build() {
            return new ProviderCapabilities(
                    supportedCpuTypes,
                    supportedStorageClasses,
                    supportedNetworkTypes,
                    supportedOsTypes,
                    resourceLimits,
                    features
            );
        }
    }
}

/**
 * Resource limits
 */
record ResourceLimits(
        int maxCpuCores,
        int maxMemoryMb,
        int maxStorageGb,
        int maxVms
) {
    static ResourceLimitsBuilder builder() {
        return new ResourceLimitsBuilder();
    }

    static class ResourceLimitsBuilder {
        private Integer maxCpuCores;
        private Integer maxMemoryMb;
        private Integer maxStorageGb;
        private Integer maxVms;

        ResourceLimitsBuilder maxCpuCores(Integer maxCpuCores) {
            this.maxCpuCores = maxCpuCores;
            return this;
        }

        ResourceLimitsBuilder maxMemoryMb(Integer maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
            return this;
        }

        ResourceLimitsBuilder maxStorageGb(Integer maxStorageGb) {
            this.maxStorageGb = maxStorageGb;
            return this;
        }

        ResourceLimitsBuilder maxVms(Integer maxVms) {
            this.maxVms = maxVms;
            return this;
        }

        ResourceLimits build() {
            return new ResourceLimits(
                    maxCpuCores != null ? maxCpuCores : 0,
                    maxMemoryMb != null ? maxMemoryMb : 0,
                    maxStorageGb != null ? maxStorageGb : 0,
                    maxVms != null ? maxVms : 0
            );
        }
    }
}

/**
 * Validation result
 */
record ValidationResult(
        boolean valid,
        List<String> errors
) {
    static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    static class ValidationResultBuilder {
        private boolean valid;
        private List<String> errors;

        ValidationResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        ValidationResultBuilder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        ValidationResult build() {
            return new ValidationResult(
                    valid,
                    errors != null ? errors : List.of()
            );
        }
    }
}

/**
 * Provider error
 */
record ProviderError(
        ErrorCode code,
        String message,
        String providerErrorCode,
        Map<String, Object> details,
        boolean retryable
) {
    enum ErrorCode {
        VALIDATION_ERROR,
        RESOURCE_UNAVAILABLE,
        QUOTA_EXCEEDED,
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        PROVIDER_ERROR,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    static ProviderErrorBuilder builder() {
        return new ProviderErrorBuilder();
    }

    static class ProviderErrorBuilder {
        private ErrorCode code;
        private String message;
        private String providerErrorCode;
        private Map<String, Object> details;
        private Boolean retryable;

        ProviderErrorBuilder code(ErrorCode code) {
            this.code = code;
            return this;
        }

        ProviderErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        ProviderErrorBuilder providerErrorCode(String providerErrorCode) {
            this.providerErrorCode = providerErrorCode;
            return this;
        }

        ProviderErrorBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        ProviderErrorBuilder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        ProviderError build() {
            return new ProviderError(
                    code,
                    message,
                    providerErrorCode,
                    details,
                    retryable != null ? retryable : false
            );
        }
    }
}

/**
 * Placement hints
 */
record PlacementHints(
        UUID datacenterId,
        UUID nodeId,
        Map<String, Object> preferences
) {
    static PlacementHintsBuilder builder() {
        return new PlacementHintsBuilder();
    }

    static class PlacementHintsBuilder {
        private UUID datacenterId;
        private UUID nodeId;
        private Map<String, Object> preferences;

        PlacementHintsBuilder datacenterId(UUID datacenterId) {
            this.datacenterId = datacenterId;
            return this;
        }

        PlacementHintsBuilder nodeId(UUID nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        PlacementHintsBuilder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        PlacementHints build() {
            return new PlacementHints(datacenterId, nodeId, preferences);
        }
    }
}
