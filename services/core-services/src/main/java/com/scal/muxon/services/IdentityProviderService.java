package com.scal.muxon.services;

import com.scal.muxon.common.Constants;
import com.scal.muxon.db.model.IdentityProviderEntity;
import com.scal.muxon.db.repository.IdentityProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityProviderService {

    @Autowired
    private IdentityProviderRepository idpRepository;

    /**
     * Return the default identity provider (all tenants share unless overridden per tenant).
     */
    public Optional<IdentityProviderEntity> getDefaultProvider() {
        return idpRepository.findById(UUID.fromString(Constants.DEFAULT_IDP_ID));
    }

    /**
     * Return provider for the given tenant. If tenant has no specific provider configured,
     * falls back to default provider.
     */
    public Optional<IdentityProviderEntity> findByTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return getDefaultProvider();
        }
        // TODO: per-tenant IDP configuration when supported (e.g. tenant_id column on identity_provider)
        return getDefaultProvider();
    }

    /**
     * Return system-level identity provider (is_system = true).
     * All tenants share this single OIDC provider.
     */
    public Optional<IdentityProviderEntity> getSystemProvider() {
        List<IdentityProviderEntity> systemProviders = idpRepository.findSystemProvider();
        return systemProviders.isEmpty() ? Optional.empty() : Optional.of(systemProviders.get(0));
    }
}
