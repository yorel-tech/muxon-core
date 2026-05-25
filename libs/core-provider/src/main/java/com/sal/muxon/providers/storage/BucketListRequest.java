package com.sal.muxon.providers.storage;

import java.util.Map;

public record BucketListRequest(
    String workspaceId,
    Map<String, String> filters
) {}
