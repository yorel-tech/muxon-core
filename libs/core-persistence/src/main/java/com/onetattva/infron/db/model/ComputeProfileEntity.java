package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a compute profile (VM spec template).
 * Compute profiles allow users to save and reuse VM specifications.
 */
@Entity
@Table(name = "compute_profile")
public class ComputeProfileEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Tenant datacenter grant that owns this profile
     */
    @Column(name = "tenant_datacenter_grant_id", nullable = false)
    private UUID tenantDatacenterGrantId;

    /**
     * Profile name (unique within tenant datacenter grant)
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Whether this is a system-level profile (available to all tenants)
     */
    @Column(name = "is_system", nullable = false)
    private boolean isSystem;

    /**
     * Profile description
     */
    @Column(name = "description")
    private String description;

    /**
     * Full VM specification as JSONB
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "spec", nullable = false, columnDefinition = "JSONB")
    private String spec;

    /**
     * Additional metadata as JSONB
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "JSONB")
    private Map<String, Object> metadata;

    /**
     * Searchable tags
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "compute_profile_tags", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "tag")
    private List<String> tags;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Optimistic locking version
     */
    @Version
    private Long version;

    // Default constructor
    public ComputeProfileEntity() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantDatacenterGrantId() {
        return tenantDatacenterGrantId;
    }

    public void setTenantDatacenterGrantId(UUID tenantDatacenterGrantId) {
        this.tenantDatacenterGrantId = tenantDatacenterGrantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
