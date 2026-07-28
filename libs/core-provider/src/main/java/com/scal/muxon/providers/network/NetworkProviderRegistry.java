package com.scal.muxon.providers.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for NetworkProvider implementations, mirroring the VmProviderRegistry pattern.
 *
 * <p>Implementations are registered by their {@link NetworkProvider#id()} and resolved
 * by the SubnetProvisioningService and VM orchestration layer.
 */
@Component
public class NetworkProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NetworkProviderRegistry.class);

    private final Map<String, NetworkProvider> providersById = new ConcurrentHashMap<>();

    public NetworkProviderRegistry(List<NetworkProvider> providerBeans) {
        for (NetworkProvider provider : providerBeans) {
            providersById.put(provider.id(), provider);
            logger.info("Registered NetworkProvider: {}", provider.id());
        }
    }

    public Optional<NetworkProvider> getProvider(String providerId) {
        return Optional.ofNullable(providersById.get(providerId));
    }

    public Map<String, NetworkProvider> getAllProviders() {
        return Map.copyOf(providersById);
    }
}
