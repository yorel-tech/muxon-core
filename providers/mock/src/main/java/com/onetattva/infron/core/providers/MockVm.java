package com.onetattva.infron.core.providers;

import com.onetattva.infron.db.enums.VmPowerState;
import com.onetattva.infron.db.enums.VmStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock VM entity for testing
 */
record MockVm(
        UUID id,
        String externalId,
        VmStatus status,
        VmPowerState powerState,
        List<String> ipAddresses,
        String hostname,
        Map<String, String> metadata,
        Instant createdAt,
        Instant updatedAt,
        String spec
) {
    static MockVmBuilder builder() {
        return new MockVmBuilder();
    }

    static class MockVmBuilder {
        private UUID id;
        private String externalId;
        private VmStatus status;
        private VmPowerState powerState;
        private List<String> ipAddresses;
        private String hostname;
        private Map<String, String> metadata;
        private Instant createdAt;
        private Instant updatedAt;
        private String spec;

        MockVmBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        MockVmBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        MockVmBuilder status(VmStatus status) {
            this.status = status;
            return this;
        }

        MockVmBuilder powerState(VmPowerState powerState) {
            this.powerState = powerState;
            return this;
        }

        MockVmBuilder ipAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
            return this;
        }

        MockVmBuilder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        MockVmBuilder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        MockVmBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        MockVmBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        MockVmBuilder spec(String spec) {
            this.spec = spec;
            return this;
        }

        MockVm build() {
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

    VmInfo toVmInfo() {
        return new VmInfo(
                externalId,
                status,
                powerState,
                ipAddresses,
                hostname,
                null,
                metadata,
                updatedAt
        );
    }
}
