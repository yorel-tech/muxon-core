package com.onetattva.infron.db.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity tracking volume attachments to resources (VMs, pods, containers).
 * <p>
 * This entity maintains a history of all volume attachments, including both
 * active and historical attachments. Active attachments have detached_at = null.
 * </p>
 * <p>
 * A volume can only be attached to one resource at a time. The unique constraint
 * ensures this while allowing historical tracking of previous attachments.
 * </p>
 */
@Entity
@Table(name = "volume_attachments", 
    indexes = {
        @Index(name = "idx_va_volume_id", columnList = "volume_id"),
        @Index(name = "idx_resource", columnList = "resource_type,resource_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_volume_resource_detached", 
            columnNames = {"volume_id", "resource_id", "detached_at"})
    }
)
public class VolumeAttachmentEntity {

    @Id
    @Column(name = "id", updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "volume_id", nullable = false)
    private UUID volumeId;

    @Column(name = "resource_type", nullable = false, length = 32)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @Column(name = "device", length = 64)
    private String device;

    @Column(name = "attached_at", nullable = false)
    private Instant attachedAt;

    @Column(name = "detached_at")
    private Instant detachedAt;

    @Version
    private Long version;

    public VolumeAttachmentEntity() {
    }

    @PrePersist
    protected void onCreate() {
        this.attachedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getVolumeId() {
        return volumeId;
    }

    public void setVolumeId(UUID volumeId) {
        this.volumeId = volumeId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public Instant getAttachedAt() {
        return attachedAt;
    }

    public void setAttachedAt(Instant attachedAt) {
        this.attachedAt = attachedAt;
    }

    public Instant getDetachedAt() {
        return detachedAt;
    }

    public void setDetachedAt(Instant detachedAt) {
        this.detachedAt = detachedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
