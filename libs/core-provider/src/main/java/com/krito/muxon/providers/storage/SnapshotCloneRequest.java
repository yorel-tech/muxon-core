package com.krito.muxon.providers.storage;

public record SnapshotCloneRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
