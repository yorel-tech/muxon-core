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

public record SnapshotCreationResult(
    boolean success,
    String snapshotId,
    String providerSnapshotId,
    long sizeBytes,
    String message,
    String errorCode) {
  public static SnapshotCreationResult success(
      String snapshotId, String providerSnapshotId, long sizeBytes) {
    return new SnapshotCreationResult(true, snapshotId, providerSnapshotId, sizeBytes, null, null);
  }

  public static SnapshotCreationResult success(String snapshotId, String providerSnapshotId) {
    return new SnapshotCreationResult(true, snapshotId, providerSnapshotId, 0, null, null);
  }

  public static SnapshotCreationResult failure(String snapshotId, String message) {
    return new SnapshotCreationResult(false, snapshotId, null, 0, message, null);
  }
}
