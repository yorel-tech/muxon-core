package com.scal.muxon.providers.storage;

public record VolumeAttachmentRequest(
    String volumeId,
    String resourceType,
    String resourceId,
    String device
) {}
