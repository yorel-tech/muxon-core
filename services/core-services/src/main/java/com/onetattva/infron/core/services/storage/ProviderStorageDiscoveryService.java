package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProvider;
import com.onetattva.infron.core.providers.storage.StorageDiscoveryProviderRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Service for discovering and synchronizing provider storage.
 * <p>
 * This service orchestrates the discovery of storage pools/classes from
 * infrastructure providers and normalizes them into the common capability model.
 * Uses the StorageDiscoveryProviderRegistry to delegate to provider-specific
 * implementations without direct SDK dependencies.
 * </p>
 */
@Service
public class ProviderStorageDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(ProviderStorageDiscoveryService.class);

    private final ProviderRepository providerRepository;
    private final ProviderStorageRepository providerStorageRepository;
    private final StorageDiscoveryProviderRegistry discoveryRegistry;

    public ProviderStorageDiscoveryService(
            ProviderRepository providerRepository,
            ProviderStorageRepository providerStorageRepository,
            StorageDiscoveryProviderRegistry discoveryRegistry) {
        this.providerRepository = providerRepository;
        this.providerStorageRepository = providerStorageRepository;
        this.discoveryRegistry = discoveryRegistry;
        
        log.info("Initialized storage discovery service with registry containing: {}", 
            discoveryRegistry.getAllProviders().keySet());
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
        
        Optional<StorageDiscoveryProvider> discoveryProvider = discoveryRegistry.getProvider(providerType);
        if (discoveryProvider.isEmpty()) {
            log.warn("No storage discovery provider found for provider type {}", providerType);
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
        
        // Discover storage asynchronously
        CompletableFuture<List<StorageDiscoveryProvider.DiscoveredStorage>> discoveryFuture = 
            discoveryProvider.get().discoverStorage(providerId, connectionInfo);
        
        List<StorageDiscoveryProvider.DiscoveredStorage> discoveredStorage;
        try {
            discoveredStorage = discoveryFuture.join();
        } catch (Exception e) {
            log.error("Failed to discover storage for provider {}: {}", providerId, e.getMessage(), e);
            return 0;
        }
        
        log.info("Discovered {} storage entries from provider {}", 
            discoveredStorage.size(), providerId);
        
        // Delete existing storage entries for this provider
        providerStorageRepository.deleteByProviderId(providerId);
        
        // Create new storage entries
        Instant syncTime = Instant.now();
        for (StorageDiscoveryProvider.DiscoveredStorage discovered : discoveredStorage) {
            ProviderStorageEntity entity = new ProviderStorageEntity();
            entity.setProviderId(providerId);
            entity.setProviderType(providerType);
            entity.setExternalId(discovered.externalId());
            entity.setName(discovered.name());
            entity.setStorageType(discovered.storageType());
            entity.setCapabilities(discovered.capabilities());
            entity.setMetrics(discovered.metrics());
            entity.setNodeId(discovered.nodeId());
            entity.setEnabled(true);
            entity.setSyncedAt(syncTime);
            
            providerStorageRepository.save(entity);
            
            log.debug("Saved provider storage: name={}, type={}, capabilities={}", 
                discovered.name(), discovered.storageType(), discovered.capabilities());
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
     * Get storage discovery provider for a provider type.
     *
     * @param providerType provider type
     * @return storage discovery provider or empty if not found
     */
    public Optional<StorageDiscoveryProvider> getDiscoveryProvider(String providerType) {
        return discoveryRegistry.getProvider(providerType);
    }

    /**
     * Check if storage discovery is supported for a provider type.
     *
     * @param providerType provider type
     * @return true if supported
     */
    public boolean isDiscoverySupported(String providerType) {
        return discoveryRegistry.isSupported(providerType);
    }

    /**
     * Test connection to a provider for storage discovery.
     *
     * @param providerType provider type
     * @param connectionInfo connection information
     * @return future with test result
     */
    public CompletableFuture<StorageDiscoveryProvider.StorageDiscoveryTestResult> testConnection(
            String providerType, Map<String, Object> connectionInfo) {
        
        Optional<StorageDiscoveryProvider> provider = discoveryRegistry.getProvider(providerType);
        if (provider.isEmpty()) {
            return CompletableFuture.completedFuture(
                StorageDiscoveryProvider.StorageDiscoveryTestResult.failure(
                    "No storage discovery provider found for type: " + providerType
                )
            );
        }
        
        return provider.get().testConnection(connectionInfo);
    }
}
