package com.krito.muxon.providers.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for storage discovery provider implementations.
 * <p>
 * Manages the registration and lookup of storage discovery providers.
 * Provider modules register their implementations on startup, and the
 * core services use this registry to discover storage from providers.
 * </p>
 */
public class StorageDiscoveryProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(StorageDiscoveryProviderRegistry.class);

    private final Map<String, StorageDiscoveryProvider> providers = new ConcurrentHashMap<>();

    /**
     * Register a storage discovery provider.
     *
     * @param provider the provider implementation to register
     */
    public void register(StorageDiscoveryProvider provider) {
        String providerType = provider.getProviderType();
        if (providers.containsKey(providerType)) {
            log.warn("Overwriting existing storage discovery provider for type: {}", providerType);
        }
        providers.put(providerType, provider);
        log.info("Registered storage discovery provider: {}", providerType);
    }

    /**
     * Unregister a storage discovery provider.
     *
     * @param providerType the provider type to unregister
     */
    public void unregister(String providerType) {
        StorageDiscoveryProvider removed = providers.remove(providerType);
        if (removed != null) {
            log.info("Unregistered storage discovery provider: {}", providerType);
        }
    }

    /**
     * Get a storage discovery provider by type.
     *
     * @param providerType the provider type
     * @return optional containing the provider if found
     */
    public Optional<StorageDiscoveryProvider> getProvider(String providerType) {
        return Optional.ofNullable(providers.get(providerType));
    }

    /**
     * Check if a provider type is supported.
     *
     * @param providerType the provider type to check
     * @return true if a provider is registered for this type
     */
    public boolean isSupported(String providerType) {
        return providers.containsKey(providerType);
    }

    /**
     * Get all registered provider types.
     *
     * @return map of provider type to provider implementation
     */
    public Map<String, StorageDiscoveryProvider> getAllProviders() {
        return Map.copyOf(providers);
    }
}
