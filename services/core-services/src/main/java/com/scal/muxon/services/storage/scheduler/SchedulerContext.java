package com.scal.muxon.services.storage.scheduler;

import java.util.Map;
import java.util.UUID;

/**
 * Context for storage scheduling decisions.
 * <p>
 * Contains all information needed to schedule storage for a volume request.
 * </p>
 */
public class SchedulerContext {
    private final String storageClassName;
    private final UUID providerId;
    private final long sizeBytes;
    private final Map<String, Object> capabilities;
    private final Map<String, Object> constraints;
    private final UUID workspaceId;
    private final UUID datacenterId;

    private SchedulerContext(Builder builder) {
        this.storageClassName = builder.storageClassName;
        this.providerId = builder.providerId;
        this.sizeBytes = builder.sizeBytes;
        this.capabilities = builder.capabilities;
        this.constraints = builder.constraints;
        this.workspaceId = builder.workspaceId;
        this.datacenterId = builder.datacenterId;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getDatacenterId() {
        return datacenterId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String storageClassName;
        private UUID providerId;
        private long sizeBytes;
        private Map<String, Object> capabilities;
        private Map<String, Object> constraints;
        private UUID workspaceId;
        private UUID datacenterId;

        public Builder storageClassName(String storageClassName) {
            this.storageClassName = storageClassName;
            return this;
        }

        public Builder providerId(UUID providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder sizeBytes(long sizeBytes) {
            this.sizeBytes = sizeBytes;
            return this;
        }

        public Builder capabilities(Map<String, Object> capabilities) {
            this.capabilities = capabilities;
            return this;
        }

        public Builder constraints(Map<String, Object> constraints) {
            this.constraints = constraints;
            return this;
        }

        public Builder workspaceId(UUID workspaceId) {
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder datacenterId(UUID datacenterId) {
            this.datacenterId = datacenterId;
            return this;
        }

        public SchedulerContext build() {
            return new SchedulerContext(this);
        }
    }
}
