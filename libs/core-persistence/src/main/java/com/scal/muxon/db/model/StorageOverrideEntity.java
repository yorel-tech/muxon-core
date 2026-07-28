package com.scal.muxon.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing manual storage class to provider storage mappings.
 * <p>
 * Storage overrides allow administrators to bypass the automatic scheduler
 * and manually map storage classes to specific provider storage pools/classes.
 * This is useful for:
 * <ul>
 *   <li>Enforcing specific storage backends for compliance</li>
 *   <li>Testing new storage configurations</li>
 *   <li>Troubleshooting scheduler issues</li>
 * </ul>
 * </p>
 * <p>
 * Example: Map "fast-ssd" storage class to specific Ceph pools:
 * <pre>
 * storage_class_name: "fast-ssd"
 * provider_type: "libvirt"
 * provider_storage_names: ["ssd_pool", "nvme_pool"]
 * </pre>
 * </p>
 */
@Entity
@Table(name = "storage_overrides",
    indexes = {
        @Index(name = "idx_storage_overrides_class", columnList = "storage_class_name"),
        @Index(name = "idx_storage_overrides_provider", columnList = "provider_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_storage_class_provider", 
            columnNames = {"storage_class_name", "provider_type"})
    }
)
public class StorageOverrideEntity {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "storage_class_name", nullable = false, length = 64)
    private String storageClassName;

    @Column(name = "provider_type", nullable = false, length = 32)
    private String providerType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "provider_storage_names", nullable = false, columnDefinition = "text[]")
    private List<String> providerStorageNames;

    @Column(name = "priority")
    private Integer priority = 100;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public StorageOverrideEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStorageClassName() {
        return storageClassName;
    }

    public void setStorageClassName(String storageClassName) {
        this.storageClassName = storageClassName;
    }

    public String getProviderType() {
        return providerType;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public List<String> getProviderStorageNames() {
        return providerStorageNames;
    }

    public void setProviderStorageNames(List<String> providerStorageNames) {
        this.providerStorageNames = providerStorageNames;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
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
