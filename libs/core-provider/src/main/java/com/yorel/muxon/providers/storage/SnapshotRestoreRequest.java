package com.yorel.muxon.providers.storage;

public record SnapshotRestoreRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
