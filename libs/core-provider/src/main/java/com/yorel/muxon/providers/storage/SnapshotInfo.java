package com.yorel.muxon.providers.storage;

import java.time.Instant;
import java.util.Map;

public record SnapshotInfo(
    String snapshotId,
    String providerSnapshotId,
    String volumeId,
    String name,
    long sizeBytes,
    String status,
    boolean immutable,
    Instant retentionUntil,
    Map<String, String> tags,
    Instant createdAt
) {}
