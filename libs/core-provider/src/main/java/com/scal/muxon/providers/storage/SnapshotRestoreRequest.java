package com.scal.muxon.providers.storage;

public record SnapshotRestoreRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
