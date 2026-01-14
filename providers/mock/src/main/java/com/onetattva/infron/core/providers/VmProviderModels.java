package com.onetattva.infron.core.providers;

import com.onetattva.infron.db.model.VmStatus;
import com.onetattva.infron.db.model.VmPowerState;
import com.onetattva.infron.db.model.VmSpec;
import com.onetattva.infron.db.model.VmResourceUsage;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request for VM creation
 */
public record VmCreationRequest(
        UUID vmId,
        VmSpec spec,
        PlacementHints placement,
        Map<String, String> metadata,
        String correlationId
) {
    public static VmCreationRequestBuilder builder() {
        return new VmCreationRequestBuilder();
    }

    public static class VmCreationRequestBuilder {
        private UUID vmId;
        private VmSpec spec;
        private PlacementHints placement;
        private Map<String, String> metadata;
        private String correlationId;

        public VmCreationRequestBuilder vmId(UUID vmId) {
            this.vmId = vmId;
            return this;
        }

        public VmCreationRequestBuilder spec(VmSpec spec) {
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

/**
 * Request for VM list
 */
public record VmListRequest(
        UUID tenantDatacenterGrantId,
        String statusFilter,
        List<String> tags,
        Integer limit
) {
    public static VmListRequestBuilder builder() {
        return new VmListRequestBuilder();
    }

    public static class VmListRequestBuilder {
        private UUID tenantDatacenterGrantId;
        private String statusFilter;
        private List<String> tags;
        private Integer limit;

        public VmListRequestBuilder tenantDatacenterGrantId(UUID tenantDatacenterGrantId) {
            this.tenantDatacenterGrantId = tenantDatacenterGrantId;
            return this;
        }

        public VmListRequestBuilder statusFilter(String statusFilter) {
            this.statusFilter = statusFilter;
            return this;
        }

        public VmListRequestBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public VmListRequestBuilder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public VmListRequest build() {
            return new VmListRequest(tenantDatacenterGrantId, statusFilter, tags, limit);
        }
    }
}

/**
 * Result of VM creation operation
 */
public record VmCreationResult(
        ResultType resultType,
        String externalVmId,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    public static VmCreationResult success(String externalVmId, VmInfo vmInfo) {
        return new VmCreationResult(ResultType.SUCCESS, externalVmId, vmInfo, null, null);
    }

    public static VmCreationResult failure(String message) {
        return new VmCreationResult(ResultType.FAILED, null, null, message, null);
    }

    public static VmCreationResult failure(ProviderError error) {
        return new VmCreationResult(ResultType.FAILED, null, null, null, error);
    }
}

/**
 * Result of VM deletion operation
 */
public record VmDeletionResult(
        ResultType resultType,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    public static VmDeletionResult success() {
        return new VmDeletionResult(ResultType.SUCCESS, null, null);
    }

    public static VmDeletionResult failure(String message) {
        return new VmDeletionResult(ResultType.FAILED, message, null);
    }

    public static VmDeletionResult failure(ProviderError error) {
        return new VmDeletionResult(ResultType.FAILED, null, error);
    }
}

/**
 * Result of VM operation (start, stop, restart, suspend, resume)
 */
public record VmOperationResult(
        ResultType resultType,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT, NOT_FOUND
    }

    public static VmOperationResult success(VmInfo vmInfo) {
        return new VmOperationResult(ResultType.SUCCESS, vmInfo, null, null);
    }

    public static VmOperationResult failure(String message) {
        return new VmOperationResult(ResultType.FAILED, null, message, null);
    }

    public static VmOperationResult failure(ProviderError error) {
        return new VmOperationResult(ResultType.FAILED, null, null, error);
    }

    public static VmOperationResult notFound() {
        return new VmOperationResult(ResultType.NOT_FOUND, null, "VM not found", null);
    }
}

/**
 * VM information
 */
public record VmInfo(
        String externalVmId,
        VmStatus status,
        VmPowerState powerState,
        List<String> ipAddresses,
        String hostname,
        VmResourceUsage resourceUsage,
        Map<String, Object> metadata,
        Instant lastUpdated
) {
    public static VmInfo fromMock(MockVm mock) {
        return new VmInfo(
                mock.externalId(),
                mock.status(),
                mock.powerState(),
                mock.ipAddresses(),
                mock.hostname(),
                null,
                mock.metadata(),
                mock.updatedAt()
        );
    }
}

/**
 * Provider capabilities
 */
public record ProviderCapabilities(
        List<String> supportedCpuTypes,
        List<String> supportedStorageClasses,
        List<String> supportedNetworkTypes,
        List<String> supportedOsTypes,
        ResourceLimits resourceLimits,
        Map<String, Object> features
) {
    public static ProviderCapabilitiesBuilder builder() {
        return new ProviderCapabilitiesBuilder();
    }

    public static class ProviderCapabilitiesBuilder {
        private List<String> supportedCpuTypes;
        private List<String> supportedStorageClasses;
        private List<String> supportedNetworkTypes;
        private List<String> supportedOsTypes;
        private ResourceLimits resourceLimits;
        private Map<String, Object> features;

        public ProviderCapabilitiesBuilder supportedCpuTypes(List<String> supportedCpuTypes) {
            this.supportedCpuTypes = supportedCpuTypes;
            return this;
        }

        public ProviderCapabilitiesBuilder supportedStorageClasses(List<String> supportedStorageClasses) {
            this.supportedStorageClasses = supportedStorageClasses;
            return this;
        }

        public ProviderCapabilitiesBuilder supportedNetworkTypes(List<String> supportedNetworkTypes) {
            this.supportedNetworkTypes = supportedNetworkTypes;
            return this;
        }

        public ProviderCapabilitiesBuilder supportedOsTypes(List<String> supportedOsTypes) {
            this.supportedOsTypes = supportedOsTypes;
            return this;
        }

        public ProviderCapabilitiesBuilder resourceLimits(ResourceLimits resourceLimits) {
            this.resourceLimits = resourceLimits;
            return this;
        }

        public ProviderCapabilitiesBuilder features(Map<String, Object> features) {
            this.features = features;
            return this;
        }

        public ProviderCapabilities build() {
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
public record ResourceLimits(
        int maxCpuCores,
        int maxMemoryMb,
        int maxStorageGb,
        int maxVms
) {
    public static ResourceLimitsBuilder builder() {
        return new ResourceLimitsBuilder();
    }

    public static class ResourceLimitsBuilder {
        private Integer maxCpuCores;
        private Integer maxMemoryMb;
        private Integer maxStorageGb;
        private Integer maxVms;

        public ResourceLimitsBuilder maxCpuCores(Integer maxCpuCores) {
            this.maxCpuCores = maxCpuCores;
            return this;
        }

        public ResourceLimitsBuilder maxMemoryMb(Integer maxMemoryMb) {
            this.maxMemoryMb = maxMemoryMb;
            return this;
        }

        public ResourceLimitsBuilder maxStorageGb(Integer maxStorageGb) {
            this.maxStorageGb = maxStorageGb;
            return this;
        }

        public ResourceLimitsBuilder maxVms(Integer maxVms) {
            this.maxVms = maxVms;
            return this;
        }

        public ResourceLimits build() {
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
public record ValidationResult(
        boolean valid,
        List<String> errors
) {
    public static ValidationResultBuilder builder() {
        return new ValidationResultBuilder();
    }

    public static class ValidationResultBuilder {
        private boolean valid;
        private List<String> errors;

        public ValidationResultBuilder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public ValidationResultBuilder errors(List<String> errors) {
            this.errors = errors;
            return this;
        }

        public ValidationResult build() {
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
public record ProviderError(
        ErrorCode code,
        String message,
        String providerErrorCode,
        Map<String, Object> details,
        boolean retryable
) {
    public enum ErrorCode {
        VALIDATION_ERROR,
        RESOURCE_UNAVAILABLE,
        QUOTA_EXCEEDED,
        NETWORK_ERROR,
        AUTHENTICATION_ERROR,
        PROVIDER_ERROR,
        TIMEOUT,
        UNKNOWN_ERROR
    }

    public static ProviderErrorBuilder builder() {
        return new ProviderErrorBuilder();
    }

    public static class ProviderErrorBuilder {
        private ErrorCode code;
        private String message;
        private String providerErrorCode;
        private Map<String, Object> details;
        private Boolean retryable;

        public ProviderErrorBuilder code(ErrorCode code) {
            this.code = code;
            return this;
        }

        public ProviderErrorBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ProviderErrorBuilder providerErrorCode(String providerErrorCode) {
            this.providerErrorCode = providerErrorCode;
            return this;
        }

        public ProviderErrorBuilder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public ProviderErrorBuilder retryable(boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        public ProviderError build() {
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
public record PlacementHints(
        UUID datacenterId,
        UUID nodeId,
        Map<String, Object> preferences
) {
    public static PlacementHintsBuilder builder() {
        return new PlacementHintsBuilder();
    }

    public static class PlacementHintsBuilder {
        private UUID datacenterId;
        private UUID nodeId;
        private Map<String, Object> preferences;

        public PlacementHintsBuilder datacenterId(UUID datacenterId) {
            this.datacenterId = datacenterId;
            return this;
        }

        public PlacementHintsBuilder nodeId(UUID nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public PlacementHintsBuilder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public PlacementHints build() {
            return new PlacementHints(datacenterId, nodeId, preferences);
        }
    }
}

/**
 * Mock VM entity for testing
 */
public record MockVm(
        UUID id,
        String externalId,
        VmStatus status,
        VmPowerState powerState,
        List<String> ipAddresses,
        String hostname,
        Map<String, Object> metadata,
        Instant createdAt,
        Instant updatedAt,
        VmSpec spec
) {
    public static MockVmBuilder builder() {
        return new MockVmBuilder();
    }

    public static class MockVmBuilder {
        private UUID id;
        private String externalId;
        private VmStatus status;
        private VmPowerState powerState;
        private List<String> ipAddresses;
        private String hostname;
        private Map<String, Object> metadata;
        private Instant createdAt;
        private Instant updatedAt;
        private VmSpec spec;

        public MockVmBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public MockVmBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public MockVmBuilder status(VmStatus status) {
            this.status = status;
            return this;
        }

        public MockVmBuilder powerState(VmPowerState powerState) {
            this.powerState = powerState;
            return this;
        }

        public MockVmBuilder ipAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
            return this;
        }

        public MockVmBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public MockVmBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public MockVmBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public MockVmBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public MockVmBuilder spec(VmSpec spec) {
            this.spec = spec;
            return this;
        }

        public MockVm build() {
            return new MockVm(
                    id,
                    externalId,
                    status,
                    powerState,
                    ipAddresses,
                    hostname,
                    metadata,
                    createdAt,
                    updatedAt,
                    spec
            );
        }
    }
}
