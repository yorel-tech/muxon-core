/**
 * Entity for tenant_datacenter_grant table.
 */
package com.krito.muxon.db.model;

import com.krito.muxon.api.model.DatacenterSettings;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_datacenter_grants")
public class TenantDatacenterGrantEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private DatacenterEntity datacenter;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean access;

    @Type(value = JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> limits;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "enabled_features", columnDefinition = "text[]")
    private List<String> enabledFeatures; // TEXT[]

    @Type(value = JsonBinaryType.class)
    @Column(name = "override_settings", columnDefinition = "jsonb")
    private DatacenterSettings overrideSettings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public TenantDatacenterGrantEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TenantEntity getTenant() {
        return tenant;
    }

    public void setTenant(TenantEntity tenant) {
        this.tenant = tenant;
    }

    public DatacenterEntity getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(DatacenterEntity datacenter) {
        this.datacenter = datacenter;
    }

    public Boolean getAccess() {
        return access;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public List<String> getEnabledFeatures() {
        return enabledFeatures;
    }

    public void setEnabledFeatures(List<String> enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public DatacenterSettings getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(DatacenterSettings overrideSettings) {
        this.overrideSettings = overrideSettings;
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
