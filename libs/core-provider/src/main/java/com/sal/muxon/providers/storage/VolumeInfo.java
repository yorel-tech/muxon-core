package com.sal.muxon.providers.storage;

import java.time.Instant;
import java.util.Map;

public record VolumeInfo(
    String volumeId,
    String providerVolumeId,
    String name,
    String storageClass,
    long sizeBytes,
    Long actualSizeBytes,
    String status,
    boolean encrypted,
    boolean thinProvisioned,
    Map<String, String> tags,
    Instant createdAt
) {}
