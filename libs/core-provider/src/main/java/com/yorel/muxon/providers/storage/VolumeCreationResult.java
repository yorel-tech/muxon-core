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

/**
 * Result of a volume creation operation.
 *
 * @param success whether the operation succeeded
 * @param volumeId Muxon volume identifier
 * @param providerVolumeId provider-specific volume identifier
 * @param volumePath volume path on the provider
 * @param sizeBytes volume size in bytes
 * @param message optional message (typically for errors)
 * @param errorCode error code if operation failed
 */
public record VolumeCreationResult(
    boolean success,
    String volumeId,
    String providerVolumeId,
    String volumePath,
    long sizeBytes,
    String message,
    String errorCode) {
  /**
   * Create a successful volume creation result.
   *
   * @param volumeId Muxon volume identifier
   * @param providerVolumeId provider-specific volume identifier
   * @param volumePath volume path on the provider
   * @param sizeBytes volume size in bytes
   * @return success result
   */
  public static VolumeCreationResult success(
      String volumeId, String providerVolumeId, String volumePath, long sizeBytes) {
    return new VolumeCreationResult(
        true, volumeId, providerVolumeId, volumePath, sizeBytes, null, null);
  }

  /**
   * Create a successful volume creation result (backward compatibility).
   *
   * @param volumeId Muxon volume identifier
   * @param providerVolumeId provider-specific volume identifier
   * @return success result
   */
  public static VolumeCreationResult success(String volumeId, String providerVolumeId) {
    return new VolumeCreationResult(true, volumeId, providerVolumeId, null, 0, null, null);
  }

  /**
   * Create a failed volume creation result.
   *
   * @param volumeId volume identifier (may be null)
   * @param message error message
   * @return failure result
   */
  public static VolumeCreationResult failure(String volumeId, String message) {
    return new VolumeCreationResult(false, volumeId, null, null, 0, message, null);
  }
}
