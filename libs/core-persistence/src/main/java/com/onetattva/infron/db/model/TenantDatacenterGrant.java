/**
 * Entity for tenant_datacenter_grant table.
 */
package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_datacenter_grant")
public class TenantDatacenterGrant {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datacenter_id", nullable = false)
    private Datacenter datacenter;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean access;

    @Column(columnDefinition = "jsonb")
    private String limits; // JSONB as String

    @Column(name = "enabled_features")
    private String[] enabledFeatures; // TEXT[]

    @Column(name = "override_settings", columnDefinition = "jsonb")
    private String overrideSettings; // JSONB as String

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public TenantDatacenterGrant() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Datacenter getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(Datacenter datacenter) {
        this.datacenter = datacenter;
    }

    public Boolean getAccess() {
        return access;
    }

    public void setAccess(Boolean access) {
        this.access = access;
    }

    public String getLimits() {
        return limits;
    }

    public void setLimits(String limits) {
        this.limits = limits;
    }

    public String[] getEnabledFeatures() {
        return enabledFeatures;
    }

    public void setEnabledFeatures(String[] enabledFeatures) {
        this.enabledFeatures = enabledFeatures;
    }

    public String getOverrideSettings() {
        return overrideSettings;
    }

    public void setOverrideSettings(String overrideSettings) {
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
