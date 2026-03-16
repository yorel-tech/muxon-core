package com.onetattva.infron.core.providers.storage;

public record VolumeAttachmentResult(
    boolean success,
    String volumeId,
    String resourceId,
    String device,
    String message,
    String errorCode
) {
    public static VolumeAttachmentResult success(String volumeId, String resourceId, String device) {
        return new VolumeAttachmentResult(true, volumeId, resourceId, device, null, null);
    }

    public static VolumeAttachmentResult failure(String message, String errorCode) {
        return new VolumeAttachmentResult(false, null, null, null, message, errorCode);
    }
}
