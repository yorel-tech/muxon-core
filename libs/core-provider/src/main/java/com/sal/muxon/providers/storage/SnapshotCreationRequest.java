package com.sal.muxon.providers.storage;

import java.time.Instant;
import java.util.Map;

public record SnapshotCreationRequest(
    String volumeId,
    String name,
    boolean immutable,
    Instant retentionUntil,
    Map<String, String> tags
) {}
