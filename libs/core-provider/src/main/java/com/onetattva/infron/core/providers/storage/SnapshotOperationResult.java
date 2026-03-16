package com.onetattva.infron.core.providers.storage;

public record SnapshotOperationResult(
    boolean success,
    String message,
    String errorCode
) {
    public static SnapshotOperationResult success() {
        return new SnapshotOperationResult(true, null, null);
    }

    public static SnapshotOperationResult failure(String message, String errorCode) {
        return new SnapshotOperationResult(false, message, errorCode);
    }
}
