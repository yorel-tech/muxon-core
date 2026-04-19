package com.krito.muxon.providers.storage;

import java.time.Instant;

public record BucketInfo(
    String bucketId,
    String providerBucketId,
    String name,
    String storageClass,
    String region,
    boolean versioning,
    BucketCreationRequest.BucketEncryption encryption,
    String acl,
    Instant createdAt
) {}
