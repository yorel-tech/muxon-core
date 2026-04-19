package com.krito.muxon.providers;

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
