package com.sal.muxon.providers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified registry for VM providers.
 * This is a basic interface that can be extended by the orchestrator service
 * to provide tenant datacenter grant resolution and caching.
 */
@Component
public class VmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(VmProviderRegistry.class);
    
    private final Map<String, VmProvider> providersById = new ConcurrentHashMap<>();

    public VmProviderRegistry(List<VmProvider> providerBeans) {
        for (VmProvider provider : providerBeans) {
            providersById.put(provider.id(), provider);
            logger.info("Registered VmProvider: {} ({})", provider.id(), provider.description());
        }
    }

    /**
     * Get a provider by ID.
     * 
     * @param providerId the provider ID
     * @return the provider if found
     */
    public Optional<VmProvider> getProvider(String providerId) {
        VmProvider provider = providersById.get(providerId);
        if (provider == null) {
            logger.warn("VmProvider not found for id: {}", providerId);
            return Optional.empty();
        }
        return Optional.of(provider);
    }

    /**
     * Get all registered providers.
     * 
     * @return all providers
     */
    public Map<String, VmProvider> getAllProviders() {
        return Map.copyOf(providersById);
    }
}
