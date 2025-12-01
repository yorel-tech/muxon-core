package com.onetattva.infron.core.services;

import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.db.repository.IdentityProviderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class IdentityProviderService {

    @Autowired
    private IdentityProviderRepository idpRepository;

    /**
     * Return provider metadata (issuer, jwksUri, protocol, id) for the given tenantId.
     * If null, return Optional.empty() to use the default provider.
     */
    public Optional<IdentityProviderEntity> getDefaultProvider() {
        return idpRepository.findById(UUID.fromString(Constants.DEFAULT_IDP_ID));
    }
}
