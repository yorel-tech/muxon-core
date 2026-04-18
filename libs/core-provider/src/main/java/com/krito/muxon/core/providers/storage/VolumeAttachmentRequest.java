package com.krito.muxon.core.providers.storage;

public record VolumeAttachmentRequest(
    String volumeId,
    String resourceType,
    String resourceId,
    String device
) {}
