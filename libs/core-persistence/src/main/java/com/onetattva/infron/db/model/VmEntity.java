package com.onetattva.infron.db.model;

import com.onetattva.infron.api.enums.VmPowerState;
import com.onetattva.infron.api.enums.VmStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing a Virtual Machine
 */
@Entity
@Table(name = "vm")
public class VmEntity {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ownership and authorization
    @Column(name = "tenant_datacenter_grant_id", nullable = false)
    private UUID tenantDatacenterGrantId;

    // VM identification
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = true)
    private String description;

    // VM specification (immutable after creation)
    @Column(name = "spec", nullable = false, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String spec;

    // Runtime state (mutable)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false)
    private VmStatus status;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "power_state", nullable = false)
    private VmPowerState powerState;

    // Provider assignment
    @Column(name = "provider_id", nullable = true)
    private UUID providerId;

    @Column(name = "node_id", nullable = true)
    private UUID nodeId;

    @Column(name = "external_id", nullable = true)
    private String externalId;

    @Column(name = "content_item_id", nullable = true)
    private UUID contentItemId;

    // Network configuration
    @Column(name = "ip_addresses", nullable = true, columnDefinition = "TEXT[]")
    private List<String> ipAddresses;

    @Column(name = "hostname", nullable = true)
    private String hostname;

    // Resource tracking
    @Column(name = "resource_usage", nullable = true, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String resourceUsage;

    // Metadata and tracking
    @Column(name = "metadata", nullable = true, columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> metadata;

    @Column(name = "tags", nullable = true, columnDefinition = "TEXT[]")
    private List<String> tags;

    // Lifecycle timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "started_at", nullable = true)
    private Instant startedAt;

    @Column(name = "stopped_at", nullable = true)
    private Instant stoppedAt;

    // Audit fields
    @Column(name = "created_by", nullable = true)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = true)
    private UUID updatedBy;

    // Version for optimistic locking
    @Version
    private Long version;

    // Constructors
    public VmEntity() {
    }

    public VmEntity(UUID id, UUID tenantDatacenterGrantId, String name, String description, String spec,
                    VmStatus status, VmPowerState powerState, UUID providerId, UUID nodeId,
                    String externalId, List<String> ipAddresses, String hostname,
                    String resourceUsage, Map<String, String> metadata, List<String> tags) {
        this.id = id;
        this.tenantDatacenterGrantId = tenantDatacenterGrantId;
        this.name = name;
        this.description = description;
        this.spec = spec;
        this.status = status;
        this.powerState = powerState;
        this.providerId = providerId;
        this.nodeId = nodeId;
        this.externalId = externalId;
        this.ipAddresses = ipAddresses;
        this.hostname = hostname;
        this.resourceUsage = resourceUsage;
        this.metadata = metadata;
        this.tags = tags;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.version = 0L;
    }

    // Getters and setters
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

    public VmStatus getStatus() {
        return status;
    }

    public void setStatus(VmStatus status) {
        this.status = status;
    }

    public VmPowerState getPowerState() {
        return powerState;
    }

    public void setPowerState(VmPowerState powerState) {
        this.powerState = powerState;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public void setNodeId(UUID nodeId) {
        this.nodeId = nodeId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public UUID getContentItemId() {
        return contentItemId;
    }

    public void setContentItemId(UUID contentItemId) {
        this.contentItemId = contentItemId;
    }

    public List<String> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(List<String> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getResourceUsage() {
        return resourceUsage;
    }

    public void setResourceUsage(String resourceUsage) {
        this.resourceUsage = resourceUsage;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getStoppedAt() {
        return stoppedAt;
    }

    public void setStoppedAt(Instant stoppedAt) {
        this.stoppedAt = stoppedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
        this.version++;
    }
}
