package com.scal.muxon.providers;

/**
 * Result of VM operation (start, stop, restart, suspend, resume)
 */
public record VmOperationResult(
        ResultType resultType,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT, NOT_FOUND
    }

    public static VmOperationResult success(VmInfo vmInfo) {
        return new VmOperationResult(ResultType.SUCCESS, vmInfo, null, null);
    }

    public static VmOperationResult failure(String message) {
        return new VmOperationResult(ResultType.FAILED, null, message, null);
    }

    public static VmOperationResult failure(ProviderError error) {
        return new VmOperationResult(ResultType.FAILED, null, null, error);
    }

    public static VmOperationResult notFound() {
        return new VmOperationResult(ResultType.NOT_FOUND, null, "VM not found", null);
    }
}
