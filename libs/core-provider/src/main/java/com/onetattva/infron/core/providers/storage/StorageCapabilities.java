package com.onetattva.infron.core.providers.storage;

import java.util.List;
import java.util.Map;

/**
 * Storage provider capabilities and resource information.
 *
 * @param supportedStorageClasses list of storage class names supported by this provider
 * @param backends map of backend storage systems with capacity information
 * @param features supported storage features
 * @param limits resource limits enforced by this provider
 */
public record StorageCapabilities(
    List<String> supportedStorageClasses,
    Map<String, StorageBackendInfo> backends,
    StorageFeatures features,
    StorageLimits limits
) {
    /**
     * Information about a storage backend.
     *
     * @param type backend type (e.g., "ceph-rbd", "zfs", "lvm")
     * @param totalCapacityBytes total storage capacity in bytes
     * @param usedCapacityBytes used storage capacity in bytes
     * @param availableCapacityBytes available storage capacity in bytes
     * @param metadata additional backend-specific metadata
     */
    public record StorageBackendInfo(
        String type,
        long totalCapacityBytes,
        long usedCapacityBytes,
        long availableCapacityBytes,
        Map<String, Object> metadata
    ) {}

    /**
     * Storage features supported by the provider.
     *
     * @param thinProvisioning whether thin provisioning is supported
     * @param encryption whether at-rest encryption is supported
     * @param snapshots whether snapshots are supported
     * @param clones whether copy-on-write clones are supported
     * @param replication whether replication is supported
     * @param qos whether QoS (IOPS/throughput limits) is supported
     * @param objectStorage whether object storage (S3-compatible) is supported
     */
    public record StorageFeatures(
        boolean thinProvisioning,
        boolean encryption,
        boolean snapshots,
        boolean clones,
        boolean replication,
        boolean qos,
        boolean objectStorage
    ) {}

    /**
     * Resource limits enforced by the provider.
     *
     * @param maxVolumeSizeBytes maximum size for a single volume
     * @param maxVolumesPerWorkspace maximum number of volumes per workspace
     * @param maxSnapshotsPerVolume maximum number of snapshots per volume
     * @param maxBucketSizeBytes maximum size for a single bucket
     */
    public record StorageLimits(
        long maxVolumeSizeBytes,
        int maxVolumesPerWorkspace,
        int maxSnapshotsPerVolume,
        long maxBucketSizeBytes
    ) {}
}
