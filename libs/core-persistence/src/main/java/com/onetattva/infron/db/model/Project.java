/**
 * Entity for project table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.db.ProjectStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_datacenter_grant_id")
    private TenantDatacenterGrant tenantDatacenterGrant;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String description;

    @Column(columnDefinition = "jsonb")
    private String labels; // JSONB as String

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "project_status default 'active'")
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    private TenantUser owner;

    @Column(name = "resource_limits", columnDefinition = "jsonb")
    private String resourceLimits; // JSONB as String

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Project() {
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

    public TenantDatacenterGrant getTenantDatacenterGrant() {
        return tenantDatacenterGrant;
    }

    public void setTenantDatacenterGrant(TenantDatacenterGrant tenantDatacenterGrant) {
        this.tenantDatacenterGrant = tenantDatacenterGrant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String labels) {
        this.labels = labels;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public TenantUser getOwner() {
        return owner;
    }

    public void setOwner(TenantUser owner) {
        this.owner = owner;
    }

    public String getResourceLimits() {
        return resourceLimits;
    }

    public void setResourceLimits(String resourceLimits) {
        this.resourceLimits = resourceLimits;
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
