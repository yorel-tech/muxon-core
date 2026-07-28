package com.yorel.muxon.providers;

import java.util.List;
import java.util.Map;

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
