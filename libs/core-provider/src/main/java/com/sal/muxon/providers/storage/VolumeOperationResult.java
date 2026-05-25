package com.sal.muxon.providers.storage;

public record VolumeOperationResult(
    boolean success,
    String message,
    String errorCode
) {
    public static VolumeOperationResult ok() {
        return new VolumeOperationResult(true, null, null);
    }

    public static VolumeOperationResult ok(String message) {
        return new VolumeOperationResult(true, message, null);
    }

    public static VolumeOperationResult failure(String message) {
        return new VolumeOperationResult(false, message, null);
    }

    public static VolumeOperationResult failure(String message, String errorCode) {
        return new VolumeOperationResult(false, message, errorCode);
    }
}
