package com.krito.muxon.providers;

/**
 * Result of VM deletion operation
 */
public record VmDeletionResult(
        ResultType resultType,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    public static VmDeletionResult success() {
        return new VmDeletionResult(ResultType.SUCCESS, null, null);
    }

    public static VmDeletionResult failure(String message) {
        return new VmDeletionResult(ResultType.FAILED, message, null);
    }

    public static VmDeletionResult failure(ProviderError error) {
        return new VmDeletionResult(ResultType.FAILED, null, error);
    }
}
