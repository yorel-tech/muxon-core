package com.krito.muxon.core.providers.storage;

public record SnapshotCreationResult(
    boolean success,
    String snapshotId,
    String providerSnapshotId,
    long sizeBytes,
    String message,
    String errorCode
) {
    public static SnapshotCreationResult success(String snapshotId, String providerSnapshotId, long sizeBytes) {
        return new SnapshotCreationResult(true, snapshotId, providerSnapshotId, sizeBytes, null, null);
    }

    public static SnapshotCreationResult success(String snapshotId, String providerSnapshotId) {
        return new SnapshotCreationResult(true, snapshotId, providerSnapshotId, 0, null, null);
    }

    public static SnapshotCreationResult failure(String snapshotId, String message) {
        return new SnapshotCreationResult(false, snapshotId, null, 0, message, null);
    }
}
