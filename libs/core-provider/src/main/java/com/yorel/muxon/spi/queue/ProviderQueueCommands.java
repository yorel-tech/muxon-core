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

/**
 * Queue command types for provider-scoped work ({@code EntityType.PROVIDER}).
 *
 * <p>Produced by core-services and consumed by the orchestrator.
 */
public final class ProviderQueueCommands {

  private ProviderQueueCommands() {}

  public static final String CONNECTION_TEST = "PROVIDER_CONNECTION_TEST_COMMAND";

  public static final String CAPABILITIES_DISCOVERY = "PROVIDER_CAPABILITIES_DISCOVERY_COMMAND";

  public static final String STORAGE_DISCOVERY = "PROVIDER_STORAGE_DISCOVERY_COMMAND";

  /** Full inventory sync: capabilities (where applicable), node cluster + nodes, storage. */
  public static final String INVENTORY_SYNC = "PROVIDER_INVENTORY_SYNC_COMMAND";

  /** Push content library artifacts to provider storage via provider API (e.g. Proxmox upload). */
  public static final String CONTENT_DATACENTER_REPLICATE = "CONTENT_DATACENTER_REPLICATE_COMMAND";
}
