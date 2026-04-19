package com.krito.muxon.providers.storage;

import java.util.Map;

/**
 * Request parameters for creating a new block volume.
 *
 * @param name volume name (must be unique within workspace)
 * @param workspaceId workspace/tenant identifier
 * @param storageClass storage class name (e.g., "fast-ssd", "balanced")
 * @param sizeBytes volume size in bytes
 * @param encrypted whether to enable at-rest encryption
 * @param encryptionKeyId encryption key identifier (null for default key)
 * @param thinProvisioned whether to use thin provisioning
 * @param tags user-defined tags for categorization
 * @param providerConfig provider-specific configuration parameters
 */
public record VolumeCreationRequest(
    String name,
    String workspaceId,
    String storageClass,
    long sizeBytes,
    boolean encrypted,
    String encryptionKeyId,
    boolean thinProvisioned,
    Map<String, String> tags,
    Map<String, Object> providerConfig
) {}
