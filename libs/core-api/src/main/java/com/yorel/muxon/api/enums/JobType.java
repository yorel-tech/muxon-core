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
package com.yorel.muxon.api.enums;

/** Job type enum */
public enum JobType {
  VM_CREATE,
  VM_DELETE,
  VM_START,
  VM_STOP,
  VM_RESTART,
  VM_SUSPEND,
  VM_RESUME,
  VM_SNAPSHOT,
  VM_BACKUP,
  VM_MIGRATE,
  VM_RESIZE,
  NODE_PROVISION,
  NODE_DECOMMISSION,
  PROVIDER_SYNC,
  /** Provider storage inventory sync (orchestrator + storage discovery providers). */
  PROVIDER_STORAGE_SYNC,
  /** Content library catalog sync from configured source (metadata only). */
  CONTENT_LIBRARY_SYNC,
  /** Content item fetch/materialization (legacy; prefer replicate jobs). */
  CONTENT_ITEM_FETCH,
  /** Pull remote artifacts into Muxon content store for a remote library. */
  CONTENT_LIBRARY_REPLICATE,
  /** Push library content from content store to provider storage at a datacenter. */
  CONTENT_DATACENTER_REPLICATE
}
