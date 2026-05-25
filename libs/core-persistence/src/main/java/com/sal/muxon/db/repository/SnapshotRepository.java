package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.SnapshotEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for snapshot entity operations.
 * <p>
 * Provides CRUD operations and queries for snapshots with soft delete support.
 * Includes special queries for immutable snapshot retention management.
 * </p>
 */
public interface SnapshotRepository extends JpaRepository<SnapshotEntity, UUID> {

    Optional<SnapshotEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<SnapshotEntity> findByVolumeIdAndDeletedAtIsNull(UUID volumeId, Pageable pageable);

    List<SnapshotEntity> findByVolumeIdAndDeletedAtIsNull(UUID volumeId);

    Page<SnapshotEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Optional<SnapshotEntity> findByProviderSnapshotId(String providerSnapshotId);

    long countByVolumeIdAndDeletedAtIsNull(UUID volumeId);

    /**
     * Find immutable snapshots whose retention period has expired.
     * <p>
     * Used by cleanup jobs to identify immutable snapshots that can now be deleted.
     * Immutable snapshots cannot be deleted before their retention period expires.
     * </p>
     *
     * @param now current timestamp
     * @return list of expired immutable snapshots
     */
    @Query("SELECT s FROM SnapshotEntity s WHERE s.immutable = true AND s.retentionUntil < :now AND s.deletedAt IS NULL")
    List<SnapshotEntity> findExpiredImmutableSnapshots(Instant now);

    @Query("SELECT SUM(s.sizeBytes) FROM SnapshotEntity s WHERE s.volumeId = :volumeId AND s.deletedAt IS NULL")
    Long sumSizeByVolumeId(UUID volumeId);
}
