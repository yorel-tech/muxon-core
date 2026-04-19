package com.krito.muxon.providers.storage;

public record BucketOperationResult(
    boolean success,
    String message,
    String errorCode
) {
    public static BucketOperationResult ok() {
        return new BucketOperationResult(true, null, null);
    }

    public static BucketOperationResult failure(String message, String errorCode) {
        return new BucketOperationResult(false, message, errorCode);
    }
}
