package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.BucketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for bucket entity operations.
 * <p>
 * Provides CRUD operations and queries for S3-compatible object storage buckets
 * with soft delete support. Bucket names must be unique within a workspace.
 * </p>
 */
public interface BucketRepository extends JpaRepository<BucketEntity, UUID> {

    Optional<BucketEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<BucketEntity> findByNameAndWorkspaceIdAndDeletedAtIsNull(String name, UUID workspaceId);

    Page<BucketEntity> findByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId, Pageable pageable);

    Page<BucketEntity> findByStorageClassAndDeletedAtIsNull(String storageClass, Pageable pageable);

    long countByWorkspaceIdAndDeletedAtIsNull(UUID workspaceId);
}
