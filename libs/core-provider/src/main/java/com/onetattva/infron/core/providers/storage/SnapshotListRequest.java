package com.onetattva.infron.core.providers.storage;

import java.util.Map;

public record SnapshotListRequest(
    String volumeId,
    Map<String, String> filters
) {}
