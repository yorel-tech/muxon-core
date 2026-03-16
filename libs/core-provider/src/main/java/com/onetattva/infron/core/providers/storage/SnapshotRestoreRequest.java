package com.onetattva.infron.core.providers.storage;

public record SnapshotRestoreRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
