package com.onetattva.infron.core.providers;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.db.model.DatacenterEntity;
import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import com.onetattva.infron.db.repository.TenantDatacenterGrantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for VM providers.
 * Manages provider instances and routes operations to appropriate providers.
 */
@Service
public class VmProviderRegistry {

    private static final Logger logger = LoggerFactory.getLogger(VmProviderRegistry.class);

    private final Map<String, VmProvider> providers = new ConcurrentHashMap<>();

    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

    /**
     * Register a new VM provider
     */
    public void registerProvider(VmProvider provider) {
        String providerId = provider.id();
        providers.put(providerId, provider);
        logger.info("Registered VM provider: {} - {}", providerId, provider.description());
    }

    /**
     * Get provider by ID
     */
    public Optional<VmProvider> getProvider(String providerId) {
        return Optional.ofNullable(providers.get(providerId));
    }

    /**
     * Get provider for a specific tenant datacenter grant
     * This queries datacenter to find appropriate provider based on providerType
     */
    public Optional<VmProvider> getProviderForTenantDatacenter(UUID tenantDatacenterGrantId) {
        if (providers.isEmpty()) {
            return Optional.empty();
        }

        // Get tenant datacenter grant
        Optional<TenantDatacenterGrantEntity> grantOpt = tenantDatacenterGrantRepository.findById(tenantDatacenterGrantId);
        if (grantOpt.isEmpty()) {
            logger.warn("No tenant datacenter grant found for ID: {}", tenantDatacenterGrantId);
            return Optional.empty();
        }

        TenantDatacenterGrantEntity grant = grantOpt.get();
        DatacenterEntity datacenter = grant.getDatacenter();

        if (datacenter == null) {
            logger.warn("Datacenter not found for grant ID: {}", tenantDatacenterGrantId);
            return Optional.empty();
        }

        // Resolve provider from datacenter's node cluster (so Libvirt/Mock/etc. is used correctly)
        if (datacenter.getNodeCluster() != null && datacenter.getNodeCluster().getProvider() != null) {
            String providerId = datacenter.getNodeCluster().getProvider().getId().toString();
            Optional<VmProvider> byId = getProvider(providerId);
            if (byId.isPresent()) {
                return byId;
            }
        }

        // Fallback: map by provider type from datacenter settings (e.g. when node cluster not linked)
        if (datacenter.getSettings() != null) {
            ProviderType providerType = datacenter.getSettings().getProviderType();
            String providerId = mapProviderTypeToProviderId(providerType);
            return getProvider(providerId);
        }

        logger.warn("No provider linked to datacenter for grant ID: {}", tenantDatacenterGrantId);
        return Optional.empty();
    }

    /**
     * Map provider type from datacenter settings to provider ID (fallback when datacenter has no node cluster)
     */
    private String mapProviderTypeToProviderId(ProviderType providerType) {
        if (providerType == null) {
            return "mock";
        }
        return switch (providerType) {
            case PROXMOX, LIBVIRT, KUBERNETES -> "mock";
            default -> "mock";
        };
    }

    /**
     * Get provider for a specific VM by external ID
     */
    public Optional<VmProvider> getProviderForVm(String externalVmId) {
        // In real implementation, this would query VM records to find provider
        // For now, return first available provider
        if (providers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(providers.values().iterator().next());
    }

    /**
     * List all registered providers
     */
    public List<VmProvider> getAllProviders() {
        return List.copyOf(providers.values());
    }

    /**
     * Remove a provider (for testing purposes)
     */
    public void unregisterProvider(String providerId) {
        VmProvider removed = providers.remove(providerId);
        if (removed != null) {
            logger.info("Unregistered VM provider: {}", providerId);
        }
    }
}
