package com.sal.muxon.providers.storage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Storage provider interface for block and object storage operations.
 * <p>
 * This SPI defines the contract between Infron's storage orchestration layer
 * and infrastructure providers (Libvirt, Proxmox, cloud providers).
 * Implementations handle provider-specific storage operations for volumes,
 * snapshots, and object storage buckets.
 * </p>
 * <p>
 * All operations are asynchronous and return CompletableFuture to support
 * non-blocking execution and parallel operations.
 * </p>
 */
public interface StorageProvider {

    /**
     * Get the unique identifier for this storage provider.
     *
     * @return provider identifier (e.g., "libvirt-node-01", "proxmox-cluster-02")
     */
    String id();

    /**
     * Get a human-readable description of this storage provider.
     *
     * @return provider description
     */
    String description();

    /**
     * Create a new block volume.
     * <p>
     * The provider should allocate storage according to the request parameters
     * and return the provider-specific volume identifier. The volume will be
     * in "creating" status initially and should transition to "available" once ready.
     * </p>
     *
     * @param request volume creation parameters including size, storage class, encryption
     * @return future with creation result containing volume and provider IDs
     */
    CompletableFuture<VolumeCreationResult> createVolume(VolumeCreationRequest request);

    /**
     * Delete an existing volume.
     * <p>
     * The provider should release all storage resources associated with the volume.
     * This operation should fail if the volume is currently attached.
     * </p>
     *
     * @param volumeId provider-specific volume identifier
     * @return future with operation result
     */
    CompletableFuture<VolumeOperationResult> deleteVolume(String volumeId);

    /**
     * Resize a volume to a new size.
     * <p>
     * The new size must be larger than the current size. The provider should
     * expand the volume while preserving existing data. Some providers may
     * require the volume to be detached for this operation.
     * </p>
     *
     * @param volumeId provider-specific volume identifier
     * @param newSizeBytes new size in bytes (must be larger than current size)
     * @return future with operation result
     */
    CompletableFuture<VolumeOperationResult> resizeVolume(String volumeId, long newSizeBytes);

    /**
     * Attach a volume to a resource (VM, pod, container).
     * <p>
     * The provider should make the volume accessible to the specified resource
     * at the given device path. The volume should transition to "in_use" status.
     * </p>
     *
     * @param request attachment parameters including resource type, ID, and device path
     * @return future with attachment result including actual device path
     */
    CompletableFuture<VolumeAttachmentResult> attachVolume(VolumeAttachmentRequest request);

    /**
     * Detach a volume from a resource.
     * <p>
     * The provider should safely disconnect the volume from the resource.
     * The volume should transition to "available" status after detachment.
     * </p>
     *
     * @param volumeId provider-specific volume identifier
     * @param resourceId resource identifier to detach from
     * @return future with operation result
     */
    CompletableFuture<VolumeOperationResult> detachVolume(String volumeId, String resourceId);

    /**
     * Get detailed information about a volume.
     *
     * @param volumeId provider-specific volume identifier
     * @return future with volume information
     */
    CompletableFuture<VolumeInfo> getVolumeInfo(String volumeId);

    /**
     * List all volumes matching the request criteria.
     *
     * @param request list parameters including workspace and filters
     * @return future with list of volume information
     */
    CompletableFuture<List<VolumeInfo>> listVolumes(VolumeListRequest request);

    /**
     * Create a point-in-time snapshot of a volume.
     * <p>
     * The provider should create a consistent snapshot of the volume's current state.
     * The snapshot can be used later for restore or clone operations.
     * </p>
     *
     * @param request snapshot parameters including volume ID and retention settings
     * @return future with snapshot creation result
     */
    CompletableFuture<SnapshotCreationResult> createSnapshot(SnapshotCreationRequest request);

    /**
     * Delete a snapshot.
     * <p>
     * The provider should release storage resources associated with the snapshot.
     * This operation should fail for immutable snapshots within their retention period.
     * </p>
     *
     * @param snapshotId provider-specific snapshot identifier
     * @return future with operation result
     */
    CompletableFuture<SnapshotOperationResult> deleteSnapshot(String snapshotId);

    /**
     * Restore a volume from a snapshot.
     * <p>
     * Creates a new volume with the contents of the specified snapshot.
     * The new volume will have the same size as the snapshot.
     * </p>
     *
     * @param request restore parameters including snapshot ID and new volume name
     * @return future with volume creation result
     */
    CompletableFuture<VolumeCreationResult> restoreFromSnapshot(SnapshotRestoreRequest request);

    /**
     * Clone a volume from a snapshot using copy-on-write.
     * <p>
     * Creates a new volume that initially shares storage with the snapshot
     * but diverges as writes occur. This is more efficient than full restore
     * for providers that support CoW (e.g., Ceph RBD, ZFS).
     * </p>
     *
     * @param request clone parameters including snapshot ID and new volume name
     * @return future with volume creation result
     */
    CompletableFuture<VolumeCreationResult> cloneFromSnapshot(SnapshotCloneRequest request);

    /**
     * List snapshots matching the request criteria.
     *
     * @param request list parameters including volume ID and filters
     * @return future with list of snapshot information
     */
    CompletableFuture<List<SnapshotInfo>> listSnapshots(SnapshotListRequest request);

    /**
     * Create an object storage bucket.
     * <p>
     * The provider should create an S3-compatible bucket with the specified
     * configuration. Returns access credentials for the bucket.
     * </p>
     *
     * @param request bucket creation parameters
     * @return future with bucket creation result including access credentials
     */
    CompletableFuture<BucketCreationResult> createBucket(BucketCreationRequest request);

    /**
     * Delete an object storage bucket.
     * <p>
     * The provider should delete the bucket and all its contents.
     * This operation should fail if the bucket is not empty (provider-dependent).
     * </p>
     *
     * @param bucketId provider-specific bucket identifier
     * @return future with operation result
     */
    CompletableFuture<BucketOperationResult> deleteBucket(String bucketId);

    /**
     * Get information about a bucket.
     *
     * @param bucketId provider-specific bucket identifier
     * @return future with bucket information
     */
    CompletableFuture<BucketInfo> getBucketInfo(String bucketId);

    /**
     * List buckets matching the request criteria.
     *
     * @param request list parameters including workspace and filters
     * @return future with list of bucket information
     */
    CompletableFuture<List<BucketInfo>> listBuckets(BucketListRequest request);

    /**
     * Get storage capabilities and available resources.
     * <p>
     * Returns information about supported storage classes, backend types,
     * available capacity, and feature support (encryption, snapshots, etc.).
     * </p>
     *
     * @return future with storage capabilities
     */
    CompletableFuture<StorageCapabilities> getCapabilities();
}
