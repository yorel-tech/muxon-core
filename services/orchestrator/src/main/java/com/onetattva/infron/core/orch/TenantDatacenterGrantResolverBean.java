package com.onetattva.infron.core.orch;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.core.providers.TenantDatacenterGrantResolver;
import com.onetattva.infron.db.model.DatacenterEntity;
import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import com.onetattva.infron.db.repository.TenantDatacenterGrantRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the VM provider ID for a tenant datacenter grant by loading grant and datacenter from the database.
 */
@ApplicationScoped
public class TenantDatacenterGrantResolverBean implements TenantDatacenterGrantResolver {

    private static final Logger logger = LoggerFactory.getLogger(TenantDatacenterGrantResolverBean.class);

    @Inject
    TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

    @Override
    public Optional<String> resolveProviderId(UUID tenantDatacenterGrantId) {
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

        if (datacenter.getNodeCluster() != null && datacenter.getNodeCluster().getProvider() != null) {
            return Optional.of(datacenter.getNodeCluster().getProvider().getId().toString());
        }

        if (datacenter.getSettings() != null) {
            ProviderType providerType = datacenter.getSettings().getProviderType();
            return Optional.of(mapProviderTypeToProviderId(providerType));
        }

        logger.warn("No provider linked to datacenter for grant ID: {}", tenantDatacenterGrantId);
        return Optional.empty();
    }

    private static String mapProviderTypeToProviderId(ProviderType providerType) {
        if (providerType == null) {
            return "mock";
        }
        return switch (providerType) {
            case PROXMOX, LIBVIRT, KUBERNETES -> "mock";
            default -> "mock";
        };
    }
}
