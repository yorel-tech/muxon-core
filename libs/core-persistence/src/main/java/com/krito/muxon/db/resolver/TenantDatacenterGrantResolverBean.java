package com.krito.muxon.db.resolver;

import com.krito.muxon.api.model.ProviderType;
import com.krito.muxon.providers.TenantDatacenterGrantResolver;
import com.krito.muxon.db.model.DatacenterEntity;
import com.krito.muxon.db.model.TenantDatacenterGrantEntity;
import com.krito.muxon.db.repository.TenantDatacenterGrantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the VM provider ID for a tenant datacenter grant by loading grant and datacenter from the database.
 */
@Service
public class TenantDatacenterGrantResolverBean implements TenantDatacenterGrantResolver {

    private static final Logger logger = LoggerFactory.getLogger(TenantDatacenterGrantResolverBean.class);

    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

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
            case PROXMOX, LIBVIRT -> "mock";
            default -> "mock";
        };
    }
}
