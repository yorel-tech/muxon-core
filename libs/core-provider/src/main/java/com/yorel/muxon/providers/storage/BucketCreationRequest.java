package com.yorel.muxon.providers.storage;

import java.util.Map;

public record BucketCreationRequest(
    String name,
    String workspaceId,
    String storageClass,
    String region,
    boolean versioning,
    BucketEncryption encryption,
    String acl,
    Map<String, Object> providerConfig
) {
    public record BucketEncryption(
        boolean enabled,
        String algorithm
    ) {}
}
