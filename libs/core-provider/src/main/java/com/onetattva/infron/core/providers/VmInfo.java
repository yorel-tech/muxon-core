package com.onetattva.infron.core.providers;

import com.onetattva.infron.api.model.VmPowerState;
import com.onetattva.infron.api.model.VmStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * VM information
 */
public record VmInfo(
        String externalVmId,
        VmStatus status,
        VmPowerState powerState,
        List<String> ipAddresses,
        String hostname,
        String resourceUsage,
        Map<String, String> metadata,
        Instant lastUpdated
) {
    public static VmInfoBuilder builder() {
        return new VmInfoBuilder();
    }

    public static class VmInfoBuilder {
        private String externalVmId;
        private VmStatus status;
        private VmPowerState powerState;
        private List<String> ipAddresses;
        private String hostname;
        private String resourceUsage;
        private Map<String, String> metadata;
        private Instant lastUpdated;

        public VmInfoBuilder externalVmId(String externalVmId) {
            this.externalVmId = externalVmId;
            return this;
        }

        public VmInfoBuilder status(VmStatus status) {
            this.status = status;
            return this;
        }

        public VmInfoBuilder powerState(VmPowerState powerState) {
            this.powerState = powerState;
            return this;
        }

        public VmInfoBuilder ipAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
            return this;
        }

        public VmInfoBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public VmInfoBuilder resourceUsage(String resourceUsage) {
            this.resourceUsage = resourceUsage;
            return this;
        }

        public VmInfoBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public VmInfoBuilder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public VmInfo build() {
            return new VmInfo(
                    externalVmId,
                    status,
                    powerState,
                    ipAddresses,
                    hostname,
                    resourceUsage,
                    metadata,
                    lastUpdated
            );
        }
    }
}
