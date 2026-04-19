package com.krito.muxon.services.storage;

import com.krito.muxon.db.model.BucketEntity;
import com.krito.muxon.db.repository.BucketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing S3-compatible object storage buckets.
 * <p>
 * Handles bucket creation, deletion, and queries. Buckets provide object
 * storage for unstructured data such as backups, media files, logs, and
 * archives. Each bucket is S3-compatible and can be accessed using standard
 * S3 APIs and tools.
 * </p>
 * <p>
 * Bucket names must be unique within a workspace.
 * </p>
 */
@Service
public class BucketService {

    private static final Logger log = LoggerFactory.getLogger(BucketService.class);

    private final BucketRepository bucketRepository;
    private final StorageClassResolutionService storageClassResolutionService;

    public BucketService(
            BucketRepository bucketRepository,
            StorageClassResolutionService storageClassResolutionService) {
        this.bucketRepository = bucketRepository;
        this.storageClassResolutionService = storageClassResolutionService;
    }

    /**
     * Create a new S3-compatible bucket.
     * <p>
     * Validates that the bucket name is unique within the workspace.
     * Bucket names must follow S3 naming conventions.
     * </p>
     *
     * @param bucket bucket entity with name, workspace, storage class, and configuration
     * @return created bucket entity
     * @throws IllegalArgumentException if bucket name already exists in workspace
     */
    @Transactional
    public BucketEntity createBucket(BucketEntity bucket) {
        log.info("Creating bucket: name={}, storageClass={}", 
            bucket.getName(), bucket.getStorageClass());

        Optional<BucketEntity> existing = bucketRepository
            .findByNameAndWorkspaceIdAndDeletedAtIsNull(bucket.getName(), bucket.getWorkspaceId());
        
        if (existing.isPresent()) {
            throw new IllegalArgumentException(
                "Bucket with name " + bucket.getName() + " already exists in workspace");
        }

        return bucketRepository.save(bucket);
    }

    /**
     * Delete a bucket (soft delete).
     * <p>
     * Marks the bucket as deleted. Note: The provider should handle
     * deletion of bucket contents before calling this method.
     * </p>
     *
     * @param bucketId bucket identifier
     * @throws IllegalArgumentException if bucket not found
     */
    @Transactional
    public void deleteBucket(UUID bucketId) {
        BucketEntity bucket = bucketRepository.findByIdAndDeletedAtIsNull(bucketId)
            .orElseThrow(() -> new IllegalArgumentException("Bucket not found: " + bucketId));

        bucket.setDeletedAt(Instant.now());
        bucketRepository.save(bucket);
        
        log.info("Deleted bucket {}", bucketId);
    }

    public Optional<BucketEntity> getBucket(UUID bucketId) {
        return bucketRepository.findByIdAndDeletedAtIsNull(bucketId);
    }

    public Optional<BucketEntity> getBucketByName(String name, UUID workspaceId) {
        return bucketRepository.findByNameAndWorkspaceIdAndDeletedAtIsNull(name, workspaceId);
    }

    public Page<BucketEntity> listBucketsByWorkspace(UUID workspaceId, Pageable pageable) {
        return bucketRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId, pageable);
    }

    public Page<BucketEntity> listBucketsByStorageClass(String storageClass, Pageable pageable) {
        return bucketRepository.findByStorageClassAndDeletedAtIsNull(storageClass, pageable);
    }

    public long countBucketsByWorkspace(UUID workspaceId) {
        return bucketRepository.countByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    }
}
