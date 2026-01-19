package com.onetattva.infron.db.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.util.Map;

/**
 * Entity for SAML 2.0 identity providers.
 */
@Entity
@DiscriminatorValue("SAML2")
public class Saml2IdentityProviderEntity extends IdentityProviderEntity {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Constructors
    public Saml2IdentityProviderEntity() {
        super();
        setProtocol(IdentityProviderProtocol.SAML2);
    }

    /**
     * Gets the SAML 2.0 metadata.
     *
     * @return the SAML 2.0 metadata
     */
    @Transient
    public Saml2Metadata getSaml2Metadata() {
        Map<String, String> metadata = getMetadata();
        if (metadata == null) {
            return null;
        }
        return objectMapper.convertValue(metadata, Saml2Metadata.class);
    }

    /**
     * Sets the SAML 2.0 metadata.
     *
     * @param saml2Metadata the SAML 2.0 metadata
     */
    public void setSaml2Metadata(Saml2Metadata saml2Metadata) {
        if (saml2Metadata == null) {
            setMetadata(null);
            return;
        }
        Map<String, String> metadata = objectMapper.convertValue(saml2Metadata, new TypeReference<Map<String, String>>() {});
        setMetadata(metadata);
    }
}
