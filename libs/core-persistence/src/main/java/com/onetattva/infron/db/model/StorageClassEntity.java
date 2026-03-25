package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Entity representing a storage class definition.
 * <p>
 * Storage classes provide a user-facing abstraction for storage capabilities
 * (e.g., "fast-ssd", "balanced", "capacity-hdd") rather than exposing
 * provider-specific implementation details. Administrators define storage
 * classes and map them to provider backends via ProviderStorageMappingEntity.
 * </p>
 * <p>
 * Storage classes define features (encryption, snapshots, thin provisioning),
 * QoS parameters (IOPS, throughput), and which providers can fulfill them.
 * </p>
 */
@Entity
@Table(name = "storage_classes")
public class StorageClassEntity {

    @Id
    @Column(name = "name", length = 64)
    private String name;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "tier", nullable = false, length = 32)
    private String tier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> features;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "qos", columnDefinition = "jsonb")
    private Map<String, Object> qos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "allowed_providers", columnDefinition = "jsonb")
    private List<String> allowedProviders;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "capabilities", columnDefinition = "jsonb")
    private Map<String, Object> capabilities;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "constraints", columnDefinition = "jsonb")
    private Map<String, Object> constraints;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public StorageClassEntity() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public Map<String, Object> getFeatures() {
        return features;
    }

    public void setFeatures(Map<String, Object> features) {
        this.features = features;
    }

    public Map<String, Object> getQos() {
        return qos;
    }

    public void setQos(Map<String, Object> qos) {
        this.qos = qos;
    }

    public List<String> getAllowedProviders() {
        return allowedProviders;
    }

    public void setAllowedProviders(List<String> allowedProviders) {
        this.allowedProviders = allowedProviders;
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

    public Map<String, Object> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Map<String, Object> capabilities) {
        this.capabilities = capabilities;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
