package com.krito.muxon.core.providers.storage;

public record VolumeAttachmentResult(
    boolean success,
    String volumeId,
    String resourceId,
    String device,
    String volumePath,
    String message,
    String errorCode
) {
    public static VolumeAttachmentResult success(String volumeId, String resourceId, String device, String volumePath) {
        return new VolumeAttachmentResult(true, volumeId, resourceId, device, volumePath, null, null);
    }

    public static VolumeAttachmentResult success(String volumeId, String resourceId, String device) {
        return new VolumeAttachmentResult(true, volumeId, resourceId, device, null, null, null);
    }

    public static VolumeAttachmentResult failure(String volumeId, String resourceId, String message) {
        return new VolumeAttachmentResult(false, volumeId, resourceId, null, null, message, null);
    }

    public static VolumeAttachmentResult failure(String message, String errorCode) {
        return new VolumeAttachmentResult(false, null, null, null, null, message, errorCode);
    }
}
