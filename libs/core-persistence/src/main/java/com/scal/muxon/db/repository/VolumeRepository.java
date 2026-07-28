package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.VolumeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for volume entity operations.
 * <p>
 * Provides CRUD operations and queries for volumes with soft delete support.
 * All query methods exclude soft-deleted volumes (deletedAt IS NULL).
 * </p>
 */
public interface VolumeRepository extends JpaRepository<VolumeEntity, UUID> {

    Optional<VolumeEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<VolumeEntity> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId, Pageable pageable);

    Page<VolumeEntity> findByWorkspaceIdAndStorageClassAndDeletedAtIsNull(
        UUID workspaceId, String storageClass, Pageable pageable);

    Page<VolumeEntity> findByProviderIdAndDeletedAtIsNull(UUID providerId, Pageable pageable);

    Page<VolumeEntity> findByStatusAndDeletedAtIsNull(String status, Pageable pageable);

    Optional<VolumeEntity> findByProviderIdAndProviderVolumeId(UUID providerId, String providerVolumeId);

    List<VolumeEntity> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);

    long countByProviderIdAndDeletedAtIsNull(UUID providerId);

    /**
     * Calculate total storage size allocated to a workspace.
     * <p>
     * Sums the size of all non-deleted volumes in a workspace.
     * Useful for quota enforcement and capacity reporting.
     * </p>
     *
     * @param workspaceId workspace identifier
     * @return total size in bytes, or null if no volumes exist
     */
    @Query("SELECT SUM(v.sizeBytes) FROM VolumeEntity v WHERE v.workspaceId = :workspaceId AND v.deletedAt IS NULL")
    Long sumSizeByWorkspaceId(UUID workspaceId);
}
