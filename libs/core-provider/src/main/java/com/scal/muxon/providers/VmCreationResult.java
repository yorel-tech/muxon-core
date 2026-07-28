package com.scal.muxon.providers;

/**
 * Result of VM creation operation
 */
public record VmCreationResult(
        ResultType resultType,
        String externalVmId,
        VmInfo vmInfo,
        String message,
        ProviderError error
) {
    public enum ResultType {
        SUCCESS, FAILED, TIMEOUT
    }

    public static VmCreationResult success(String externalVmId, VmInfo vmInfo) {
        return new VmCreationResult(ResultType.SUCCESS, externalVmId, vmInfo, null, null);
    }

    public static VmCreationResult failure(String message) {
        return new VmCreationResult(ResultType.FAILED, null, null, message, null);
    }

    public static VmCreationResult failure(ProviderError error) {
        return new VmCreationResult(ResultType.FAILED, null, null, null, error);
    }
}
