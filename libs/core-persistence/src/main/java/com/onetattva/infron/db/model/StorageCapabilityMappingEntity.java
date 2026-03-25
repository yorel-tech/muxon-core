package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity mapping Infron generic capabilities to provider-specific equivalents.
 * <p>
 * This entity enables translation between Infron's generic storage capability
 * model and provider-specific terminology. For example:
 * <ul>
 *   <li>Infron "performance: high" → Libvirt pool_type: ["rbd", "nvme"]</li>
 *   <li>Infron "performance: high" → Proxmox storage_type: ["rbd", "zfspool"]</li>
 * </ul>
 * </p>
 */
@Entity
@Table(name = "storage_capability_mappings",
    indexes = {
        @Index(name = "idx_scm_infron_capability", columnList = "infron_capability"),
        @Index(name = "idx_scm_provider_type", columnList = "provider_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_capability_provider", 
            columnNames = {"infron_capability", "provider_type", "provider_capability"})
    }
)
public class StorageCapabilityMappingEntity {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "infron_capability", nullable = false, length = 64)
    private String infronCapability;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @Column(name = "provider_capability", nullable = false, length = 64)
    private String providerCapability;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_mapping", columnDefinition = "jsonb")
    private Map<String, Object> valueMapping;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public StorageCapabilityMappingEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInfronCapability() {
        return infronCapability;
    }

    public void setInfronCapability(String infronCapability) {
        this.infronCapability = infronCapability;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public String getProviderCapability() {
        return providerCapability;
    }

    public void setProviderCapability(String providerCapability) {
        this.providerCapability = providerCapability;
    }

    public Map<String, Object> getValueMapping() {
        return valueMapping;
    }

    public void setValueMapping(Map<String, Object> valueMapping) {
        this.valueMapping = valueMapping;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
