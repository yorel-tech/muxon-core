package com.krito.muxon.db.repository;

import com.krito.muxon.db.model.VolumeAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for volume attachment entity operations.
 * <p>
 * Tracks volume attachments to resources (VMs, pods, containers).
 * Active attachments have detachedAt = null. Historical attachments
 * are preserved for audit purposes.
 * </p>
 */
public interface VolumeAttachmentRepository extends JpaRepository<VolumeAttachmentEntity, UUID> {

    List<VolumeAttachmentEntity> findByVolumeIdAndDetachedAtIsNull(UUID volumeId);

    Optional<VolumeAttachmentEntity> findByVolumeIdAndResourceIdAndDetachedAtIsNull(
        UUID volumeId, UUID resourceId);

    List<VolumeAttachmentEntity> findByResourceTypeAndResourceIdAndDetachedAtIsNull(
        String resourceType, UUID resourceId);

    @Query("SELECT va FROM VolumeAttachmentEntity va WHERE va.volumeId = :volumeId AND va.detachedAt IS NULL")
    Optional<VolumeAttachmentEntity> findActiveAttachmentByVolumeId(UUID volumeId);

    boolean existsByVolumeIdAndDetachedAtIsNull(UUID volumeId);
}
