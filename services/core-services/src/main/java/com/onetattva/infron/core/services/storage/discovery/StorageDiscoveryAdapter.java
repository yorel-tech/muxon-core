package com.onetattva.infron.core.services.storage.discovery;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Interface for provider-specific storage discovery.
 * <p>
 * Implementations discover and normalize storage pools/classes from
 * infrastructure providers (Libvirt, Proxmox) into a common capability model.
 * </p>
 */
public interface StorageDiscoveryAdapter {

    /**
     * Get the provider type this adapter handles.
     *
     * @return provider type (libvirt, proxmox)
     */
    String getProviderType();

    /**
     * Discover storage pools/classes from a provider.
     * <p>
     * Connects to the provider and retrieves all available storage
     * configurations, normalizing them into the common model.
     * </p>
     *
     * @param providerId provider identifier
     * @param connectionInfo provider connection information
     * @return list of discovered storage entries
     */
    List<DiscoveredStorage> discoverStorage(UUID providerId, Map<String, Object> connectionInfo);

    /**
     * Represents a discovered storage pool/class from a provider.
     */
    class DiscoveredStorage {
        private String externalId;
        private String name;
        private String storageType;
        private Map<String, Object> capabilities;
        private Map<String, Object> metrics;
        private String nodeId;

        public DiscoveredStorage() {
        }

        public DiscoveredStorage(String externalId, String name, String storageType,
                                Map<String, Object> capabilities, Map<String, Object> metrics,
                                String nodeId) {
            this.externalId = externalId;
            this.name = name;
            this.storageType = storageType;
            this.capabilities = capabilities;
            this.metrics = metrics;
            this.nodeId = nodeId;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStorageType() {
            return storageType;
        }

        public void setStorageType(String storageType) {
            this.storageType = storageType;
        }

        public Map<String, Object> getCapabilities() {
            return capabilities;
        }

        public void setCapabilities(Map<String, Object> capabilities) {
            this.capabilities = capabilities;
        }

        public Map<String, Object> getMetrics() {
            return metrics;
        }

        public void setMetrics(Map<String, Object> metrics) {
            this.metrics = metrics;
        }

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }
    }
}
