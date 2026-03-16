package com.onetattva.infron.core.providers.storage;

public record VolumeOperationResult(
    boolean success,
    String message,
    String errorCode
) {
    public static VolumeOperationResult success() {
        return new VolumeOperationResult(true, null, null);
    }

    public static VolumeOperationResult success(String message) {
        return new VolumeOperationResult(true, message, null);
    }

    public static VolumeOperationResult failure(String message, String errorCode) {
        return new VolumeOperationResult(false, message, errorCode);
    }
}
