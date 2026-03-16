package com.onetattva.infron.core.providers.storage;

/**
 * Result of a volume creation operation.
 *
 * @param success whether the operation succeeded
 * @param volumeId Infron volume identifier
 * @param providerVolumeId provider-specific volume identifier
 * @param message optional message (typically for errors)
 * @param errorCode error code if operation failed
 */
public record VolumeCreationResult(
    boolean success,
    String volumeId,
    String providerVolumeId,
    String message,
    String errorCode
) {
    /**
     * Create a successful volume creation result.
     *
     * @param volumeId Infron volume identifier
     * @param providerVolumeId provider-specific volume identifier
     * @return success result
     */
    public static VolumeCreationResult success(String volumeId, String providerVolumeId) {
        return new VolumeCreationResult(true, volumeId, providerVolumeId, null, null);
    }

    /**
     * Create a failed volume creation result.
     *
     * @param message error message
     * @param errorCode error code
     * @return failure result
     */
    public static VolumeCreationResult failure(String message, String errorCode) {
        return new VolumeCreationResult(false, null, null, message, errorCode);
    }
}
