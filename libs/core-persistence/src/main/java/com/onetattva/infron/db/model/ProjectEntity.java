/**
 * Entity for project table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.model.ResourceLimits;
import com.onetattva.infron.db.ProjectStatus;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.Type;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "project")
public class ProjectEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantEntity tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_datacenter_grant_id")
    private TenantDatacenterGrantEntity tenantDatacenterGrant;

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "project_status default 'active'")
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner")
    private TenantUserEntity owner;

    @Type(value = JsonBinaryType.class)
    @Column(name = "resource_limits", columnDefinition = "jsonb")
    private ResourceLimits resourceLimits;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public ProjectEntity() {
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

    public TenantDatacenterGrantEntity getTenantDatacenterGrant() {
        return tenantDatacenterGrant;
    }

    public void setTenantDatacenterGrant(TenantDatacenterGrantEntity tenantDatacenterGrant) {
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

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public TenantUserEntity getOwner() {
        return owner;
    }

    public void setOwner(TenantUserEntity owner) {
        this.owner = owner;
    }

    public ResourceLimits getResourceLimits() {
        return resourceLimits;
    }

    public void setResourceLimits(ResourceLimits resourceLimits) {
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
