/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.providers.storage;

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
    Map<String, Object> providerConfig) {}
