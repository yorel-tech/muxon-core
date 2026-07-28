package com.scal.muxon.providers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing storage provider instances.
 * <p>
 * This registry maintains a thread-safe collection of storage providers
 * and allows lookup by provider ID. Providers can be dynamically registered
 * and unregistered at runtime.
 * </p>
 */
public class StorageProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(StorageProviderRegistry.class);

    private final Map<String, StorageProvider> providers = new ConcurrentHashMap<>();

    /**
     * Register a storage provider.
     * <p>
     * If a provider with the same ID already exists, it will be replaced
     * and a warning will be logged.
     * </p>
     *
     * @param provider the storage provider to register
     */
    public void register(StorageProvider provider) {
        String providerId = provider.id();
        if (providers.containsKey(providerId)) {
            log.warn("Storage provider {} is already registered, replacing", providerId);
        }
        providers.put(providerId, provider);
        log.info("Registered storage provider: {} - {}", providerId, provider.description());
    }

    /**
     * Unregister a storage provider by ID.
     * <p>
     * If the provider doesn't exist, a warning will be logged.
     * </p>
     *
     * @param providerId the provider ID to unregister
     */
    public void unregister(String providerId) {
        StorageProvider removed = providers.remove(providerId);
        if (removed != null) {
            log.info("Unregistered storage provider: {}", providerId);
        } else {
            log.warn("Attempted to unregister non-existent storage provider: {}", providerId);
        }
    }

    public Optional<StorageProvider> getProvider(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    public Map<String, StorageProvider> getAllProviders() {
        return Map.copyOf(providers);
    }

    public boolean hasProvider(String providerId) {
        return providers.containsKey(providerId);
    }

    public int getProviderCount() {
        return providers.size();
    }
}
