package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing an S3-compatible object storage bucket.
 * <p>
 * Buckets provide object storage for unstructured data such as backups,
 * media files, logs, and archives. Each bucket is S3-compatible and can
 * be accessed using standard S3 APIs and tools.
 * </p>
 * <p>
 * Buckets support versioning, encryption, ACLs, and lifecycle policies
 * (in Enterprise edition) for automatic data tiering and expiration.
 * </p>
 */
@Entity
@Table(name = "buckets",
    indexes = {
        @Index(name = "idx_workspace_id", columnList = "workspace_id"),
        @Index(name = "idx_storage_class", columnList = "storage_class")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_name_workspace", 
            columnNames = {"name", "workspace_id"})
    }
)
public class BucketEntity {

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

    @Column(name = "region", length = 64)
    private String region;

    @Column(name = "versioning")
    private Boolean versioning = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "encryption", columnDefinition = "jsonb")
    private Map<String, Object> encryption;

    @Column(name = "acl", length = 32)
    private String acl = "private";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "lifecycle_rules", columnDefinition = "jsonb")
    private Map<String, Object> lifecycleRules;

    @Column(name = "provider_bucket_id", nullable = false)
    private String providerBucketId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    private Long version;

    public BucketEntity() {
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
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

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Boolean getVersioning() {
        return versioning;
    }

    public void setVersioning(Boolean versioning) {
        this.versioning = versioning;
    }

    public Map<String, Object> getEncryption() {
        return encryption;
    }

    public void setEncryption(Map<String, Object> encryption) {
        this.encryption = encryption;
    }

    public String getAcl() {
        return acl;
    }

    public void setAcl(String acl) {
        this.acl = acl;
    }

    public Map<String, Object> getLifecycleRules() {
        return lifecycleRules;
    }

    public void setLifecycleRules(Map<String, Object> lifecycleRules) {
        this.lifecycleRules = lifecycleRules;
    }

    public String getProviderBucketId() {
        return providerBucketId;
    }

    public void setProviderBucketId(String providerBucketId) {
        this.providerBucketId = providerBucketId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
