package com.krito.muxon.core.services.storage;

import com.krito.muxon.core.services.storage.scheduler.CapabilityFilter;
import com.krito.muxon.db.model.ProviderStorageEntity;
import com.krito.muxon.db.model.StorageClassEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Capability-based compatibility between provider storage and a storage class for mapping UX.
 * <p>
 * Provider capabilities must be a subset of the class capability map: every class key must be
 * satisfied by the provider, and the provider must not advertise keys absent from the class.
 * Uses the same per-value rules as {@link CapabilityFilter} ({@code any} / {@code *} on the class
 * side). {@link CapabilityFilter#meetsConstraintsForListing} is applied for IOPS / latency-style
 * constraints (volume size treated as 0 so free-space is not required).
 * </p>
 */
@Component
public class ProviderStorageClassMappingEvaluator {

    private final CapabilityFilter capabilityFilter;

    public ProviderStorageClassMappingEvaluator(CapabilityFilter capabilityFilter) {
        this.capabilityFilter = capabilityFilter;
    }

    /**
     * True if this provider storage is mapped to the class by capability rules (excluding overrides).
     */
    public boolean matchesByCapabilities(StorageClassEntity storageClass, ProviderStorageEntity storage) {
        Map<String, Object> required = storageClass.getCapabilities();
        Map<String, Object> actual = storage.getCapabilities();
        if (required == null || required.isEmpty()) {
            return false;
        }
        if (actual == null || actual.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : required.entrySet()) {
            if (!matchesValue(entry.getValue(), actual.get(entry.getKey()))) {
                return false;
            }
        }
        for (String providerKey : actual.keySet()) {
            if (!required.containsKey(providerKey)) {
                return false;
            }
            if (!matchesValue(required.get(providerKey), actual.get(providerKey))) {
                return false;
            }
        }
        return capabilityFilter.meetsConstraintsForListing(storage, storageClass);
    }

    private static boolean matchesValue(Object required, Object actual) {
        if (required == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        if ("any".equals(String.valueOf(required)) || "*".equals(String.valueOf(required))) {
            return true;
        }
        return String.valueOf(required).equals(String.valueOf(actual));
    }
}
