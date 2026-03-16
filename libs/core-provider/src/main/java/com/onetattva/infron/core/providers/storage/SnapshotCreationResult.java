package com.onetattva.infron.core.providers.storage;

public record SnapshotCreationResult(
    boolean success,
    String snapshotId,
    String providerSnapshotId,
    String message,
    String errorCode
) {
    public static SnapshotCreationResult success(String snapshotId, String providerSnapshotId) {
        return new SnapshotCreationResult(true, snapshotId, providerSnapshotId, null, null);
    }

    public static SnapshotCreationResult failure(String message, String errorCode) {
        return new SnapshotCreationResult(false, null, null, message, errorCode);
    }
}
