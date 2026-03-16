package com.onetattva.infron.core.providers.storage;

public record SnapshotCloneRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
