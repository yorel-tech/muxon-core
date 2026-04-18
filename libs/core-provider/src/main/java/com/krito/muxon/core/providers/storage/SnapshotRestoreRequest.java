package com.krito.muxon.core.providers.storage;

public record SnapshotRestoreRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
