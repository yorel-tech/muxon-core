package com.onetattva.infron.db.model;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Map;

/**
 * Entity for OIDC identity providers.
 */
@Entity
@DiscriminatorValue("OIDC")
public class OidcIdentityProviderEntity extends IdentityProviderEntity {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructors
    public OidcIdentityProviderEntity() {
        super();
        setProtocol(IdentityProviderProtocol.OIDC);
    }

    /**
     * Gets the OIDC metadata.
     *
     * @return the OIDC metadata
     */
    public OidcMetadata getOidcMetadata() {
        Map<String, String> metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        return objectMapper.convertValue(metadata, OidcMetadata.class);
    }

    /**
     * Sets the OIDC metadata.
     *
     * @param oidcMetadata the OIDC metadata
     */
    public void setOidcMetadata(OidcMetadata oidcMetadata) {
        if (oidcMetadata == null) {
            setMetadata(null);
            return;
        }
        Map<String, String> metadata = objectMapper.convertValue(oidcMetadata, new TypeReference<Map<String, String>>() {});
        setMetadata(metadata);
    }
}
