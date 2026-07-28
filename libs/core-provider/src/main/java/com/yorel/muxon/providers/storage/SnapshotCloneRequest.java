package com.yorel.muxon.providers.storage;

public record SnapshotCloneRequest(
    String snapshotId,
    String volumeName,
    String storageClass
) {}
