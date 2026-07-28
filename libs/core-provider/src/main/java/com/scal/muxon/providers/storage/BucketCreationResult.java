package com.scal.muxon.providers.storage;

public record BucketCreationResult(
    boolean success,
    String bucketId,
    String providerBucketId,
    String endpoint,
    String accessKeyId,
    String secretAccessKey,
    String message,
    String errorCode
) {
    public static BucketCreationResult success(String bucketId, String providerBucketId, 
                                               String endpoint, String accessKeyId, String secretAccessKey) {
        return new BucketCreationResult(true, bucketId, providerBucketId, endpoint, 
                                       accessKeyId, secretAccessKey, null, null);
    }

    public static BucketCreationResult failure(String message, String errorCode) {
        return new BucketCreationResult(false, null, null, null, null, null, message, errorCode);
    }
}
