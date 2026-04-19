package com.krito.muxon.providers.storage;

import java.util.Map;

public record VolumeListRequest(
    String workspaceId,
    String storageClass,
    Map<String, String> filters
) {}
