package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import com.onetattva.infron.core.services.storage.discovery.StorageDiscoveryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Service for discovering and synchronizing provider storage.
 * <p>
 * This service orchestrates the discovery of storage pools/classes from
 * infrastructure providers and normalizes them into the common capability model.
 * </p>
 */
@Service
public class ProviderStorageDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageDiscoveryService.class);

    private final ProviderRepository providerRepository;
    private final ProviderStorageRepository providerStorageRepository;
    private final Map<String, StorageDiscoveryAdapter> discoveryAdapters;

    public ProviderStorageDiscoveryService(
            ProviderRepository providerRepository,
            ProviderStorageRepository providerStorageRepository,
            List<StorageDiscoveryAdapter> adapters) {
        this.providerRepository = providerRepository;
        this.providerStorageRepository = providerStorageRepository;
        
        // Build adapter map by provider type
        this.discoveryAdapters = new HashMap<>();
        for (StorageDiscoveryAdapter adapter : adapters) {
            discoveryAdapters.put(adapter.getProviderType(), adapter);
        }
        
        log.info("Initialized storage discovery with {} adapters: {}", 
            adapters.size(), discoveryAdapters.keySet());
    }

    /**
     * Discover and synchronize storage for a provider.
     * <p>
     * This method:
     * <ol>
     *   <li>Discovers storage from the provider using the appropriate adapter</li>
     *   <li>Deletes existing provider storage entries</li>
     *   <li>Creates new entries with discovered storage</li>
     *   <li>Updates sync timestamp</li>
     * </ol>
     * </p>
     *
     * @param providerId provider identifier
     * @return number of storage entries discovered
     */
    @Transactional
    public int discoverAndSyncStorage(UUID providerId) {
        log.info("Starting storage discovery for provider {}", providerId);
        
        Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
        if (providerOpt.isEmpty()) {
            log.warn("Provider {} not found", providerId);
            return 0;
        }
        
        ProviderEntity provider = providerOpt.get();
        String providerType = provider.getType().toString().toLowerCase();
        
        StorageDiscoveryAdapter adapter = discoveryAdapters.get(providerType);
        if (adapter == null) {
            log.warn("No storage discovery adapter found for provider type {}", providerType);
            return 0;
        }
        
        // Prepare connection info
        Map<String, Object> connectionInfo = new HashMap<>();
        connectionInfo.put("endpoint", provider.getEndpoint());
        connectionInfo.put("credentials", provider.getCredentials());
        
        // For Libvirt, add URI
        if ("libvirt".equals(providerType)) {
            connectionInfo.put("uri", provider.getEndpoint());
        }
        
        // Discover storage
        List<StorageDiscoveryAdapter.DiscoveredStorage> discoveredStorage = 
            adapter.discoverStorage(providerId, connectionInfo);
        
        log.info("Discovered {} storage entries from provider {}", 
            discoveredStorage.size(), providerId);
        
        // Delete existing storage entries for this provider
        providerStorageRepository.deleteByProviderId(providerId);
        
        // Create new storage entries
        Instant syncTime = Instant.now();
        for (StorageDiscoveryAdapter.DiscoveredStorage discovered : discoveredStorage) {
            ProviderStorageEntity entity = new ProviderStorageEntity();
            entity.setProviderId(providerId);
            entity.setProviderType(providerType);
            entity.setExternalId(discovered.getExternalId());
            entity.setName(discovered.getName());
            entity.setStorageType(discovered.getStorageType());
            entity.setCapabilities(discovered.getCapabilities());
            entity.setMetrics(discovered.getMetrics());
            entity.setNodeId(discovered.getNodeId());
            entity.setEnabled(true);
            entity.setSyncedAt(syncTime);
            
            providerStorageRepository.save(entity);
            
            log.debug("Saved provider storage: name={}, type={}, capabilities={}", 
                discovered.getName(), discovered.getStorageType(), discovered.getCapabilities());
        }
        
        log.info("Successfully synced {} storage entries for provider {}", 
            discoveredStorage.size(), providerId);
        
        return discoveredStorage.size();
    }

    /**
     * Discover storage for all providers.
     *
     * @return map of provider ID to number of discovered storage entries
     */
    @Transactional
    public Map<UUID, Integer> discoverAllProviders() {
        Map<UUID, Integer> results = new HashMap<>();
        
        List<ProviderEntity> providers = providerRepository.findAll();
        log.info("Discovering storage for {} providers", providers.size());
        
        for (ProviderEntity provider : providers) {
            try {
                int count = discoverAndSyncStorage(provider.getId());
                results.put(provider.getId(), count);
            } catch (Exception e) {
                log.error("Failed to discover storage for provider {}: {}", 
                    provider.getId(), e.getMessage(), e);
                results.put(provider.getId(), 0);
            }
        }
        
        return results;
    }

    /**
     * Get all discovered storage for a provider.
     *
     * @param providerId provider identifier
     * @return list of provider storage entities
     */
    public List<ProviderStorageEntity> getProviderStorage(UUID providerId) {
        return providerStorageRepository.findByProviderId(providerId);
    }

    /**
     * Get all enabled storage for a provider.
     *
     * @param providerId provider identifier
     * @return list of enabled provider storage entities
     */
    public List<ProviderStorageEntity> getEnabledProviderStorage(UUID providerId) {
        return providerStorageRepository.findByProviderIdAndEnabled(providerId, true);
    }

    /**
     * Check if a provider has discovered storage.
     *
     * @param providerId provider identifier
     * @return true if provider has storage entries
     */
    public boolean hasDiscoveredStorage(UUID providerId) {
        return !providerStorageRepository.findByProviderId(providerId).isEmpty();
    }

    /**
     * Get storage discovery adapter for a provider type.
     *
     * @param providerType provider type
     * @return storage discovery adapter or null if not found
     */
    public StorageDiscoveryAdapter getAdapter(String providerType) {
        return discoveryAdapters.get(providerType);
    }

    /**
     * Check if storage discovery is supported for a provider type.
     *
     * @param providerType provider type
     * @return true if supported
     */
    public boolean isDiscoverySupported(String providerType) {
        return discoveryAdapters.containsKey(providerType);
    }
}
