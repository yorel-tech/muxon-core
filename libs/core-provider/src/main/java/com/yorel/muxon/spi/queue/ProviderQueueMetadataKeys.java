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
package com.yorel.muxon.spi.queue;

/** Metadata keys on {@link CommandMessage} for provider-scoped queue commands. */
public final class ProviderQueueMetadataKeys {

  private ProviderQueueMetadataKeys() {}

  /**
   * {@link java.util.UUID} of {@code job} row; orchestrator updates job status when work finishes.
   */
  public static final String JOB_ID = "jobId";

  /** Max seconds for provider-side discovery work (orchestrator enforces). */
  public static final String EXECUTION_TIMEOUT_SECONDS = "executionTimeoutSeconds";

  /** Absolute artifact root on the core-services host (resolved before enqueue). */
  public static final String ARTIFACT_ROOT = "artifactRoot";

  /**
   * Comma-separated Proxmox storage names ({@code ProviderStorageEntity.externalId}) to replicate
   * into.
   */
  public static final String STORAGE_POOL_IDS = "storagePoolIds";

  public static final String LIBRARY_ID = "libraryId";

  public static final String DATACENTER_ID = "datacenterId";

  /**
   * {@link java.util.UUID} of {@code content_library_distribution} row (core-services owns this
   * table).
   */
  public static final String DISTRIBUTION_ID = "distributionId";
}
