package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a persistent block storage volume.
 * <p>
 * Volumes are the primary storage abstraction in Infron, providing persistent
 * block storage that can be attached to VMs, pods, or containers. Each volume
 * belongs to a workspace and is backed by a specific storage provider.
 * </p>
 * <p>
 * Lifecycle states: creating → available → attaching → in_use → detaching → available → deleting → deleted
 * </p>
 */
@Entity
@Table(name = "volumes", indexes = {
    @Index(name = "idx_workspace_id", columnList = "workspace_id"),
    @Index(name = "idx_provider_id", columnList = "provider_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_storage_class", columnList = "storage_class")
})
public class VolumeEntity {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "storage_class", nullable = false, length = 64)
    private String storageClass;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "actual_size_bytes")
    private Long actualSizeBytes;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "provider_id", nullable = false)
    private UUID providerId;

    @Column(name = "provider_volume_id", nullable = false)
    private String providerVolumeId;

    @Column(name = "encrypted", nullable = false)
    private Boolean encrypted = false;

    @Column(name = "encryption_key_id", length = 64)
    private String encryptionKeyId;

    @Column(name = "thin_provisioned", nullable = false)
    private Boolean thinProvisioned = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "jsonb")
    private Map<String, String> tags;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    public VolumeEntity() {
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Long getActualSizeBytes() {
        return actualSizeBytes;
    }

    public void setActualSizeBytes(Long actualSizeBytes) {
        this.actualSizeBytes = actualSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getProviderId() {
        return providerId;
    }

    public void setProviderId(UUID providerId) {
        this.providerId = providerId;
    }

    public String getProviderVolumeId() {
        return providerVolumeId;
    }

    public void setProviderVolumeId(String providerVolumeId) {
        this.providerVolumeId = providerVolumeId;
    }

    public Boolean getEncrypted() {
        return encrypted;
    }

    public void setEncrypted(Boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public Boolean getThinProvisioned() {
        return thinProvisioned;
    }

    public void setThinProvisioned(Boolean thinProvisioned) {
        this.thinProvisioned = thinProvisioned;
    }

    public Map<String, String> getTags() {
        return tags;
    }

    public void setTags(Map<String, String> tags) {
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
