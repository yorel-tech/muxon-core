/**
 * Entity for tenant_user table.
 */
package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_user")
public class TenantUser {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "external_id")
    private String externalId; // OIDC sub

    @Column(name = "external_issuer")
    private String externalIssuer; // OIDC issuer

    private String username;

    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(columnDefinition = "jsonb")
    private String metadata; // JSONB as String

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public TenantUser() {
    }

    public TenantUser(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalIssuer() {
        return externalIssuer;
    }

    public void setExternalIssuer(String externalIssuer) {
        this.externalIssuer = externalIssuer;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
