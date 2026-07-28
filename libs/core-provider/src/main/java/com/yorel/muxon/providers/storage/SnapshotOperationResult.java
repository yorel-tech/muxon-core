package com.yorel.muxon.providers.storage;

public record SnapshotOperationResult(
    boolean success,
    String message,
    String errorCode
) {
    public static SnapshotOperationResult ok() {
        return new SnapshotOperationResult(true, null, null);
    }

    public static SnapshotOperationResult ok(String message) {
        return new SnapshotOperationResult(true, message, null);
    }

    public static SnapshotOperationResult failure(String message) {
        return new SnapshotOperationResult(false, message, null);
    }

    public static SnapshotOperationResult failure(String message, String errorCode) {
        return new SnapshotOperationResult(false, message, errorCode);
    }
}
