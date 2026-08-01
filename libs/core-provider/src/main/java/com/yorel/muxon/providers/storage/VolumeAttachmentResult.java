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

public record VolumeAttachmentResult(
    boolean success,
    String volumeId,
    String resourceId,
    String device,
    String volumePath,
    String message,
    String errorCode) {
  public static VolumeAttachmentResult success(
      String volumeId, String resourceId, String device, String volumePath) {
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
