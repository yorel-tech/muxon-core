package com.krito.muxon.core.providers.storage;

public record SnapshotCloneRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
