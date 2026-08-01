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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage discovery provider interface for discovering available storage from infrastructure
 * providers.
 *
 * <p>This SPI defines the contract for discovering storage pools, volumes, and capabilities from
 * infrastructure providers (Libvirt, Proxmox, cloud providers). Implementations handle
 * provider-specific API calls to enumerate storage resources.
 *
 * <p>All operations are asynchronous and return CompletableFuture to support non-blocking execution
 * and parallel discovery across multiple providers.
 */
public interface StorageDiscoveryProvider {

  /**
   * Get the provider type identifier.
   *
   * @return provider type (e.g., "libvirt", "proxmox", "aws", "gcp")
   */
  String getProviderType();

  /**
   * Discover all available storage from the provider.
   *
   * <p>Connects to the provider using the provided connection information and enumerates all
   * storage pools, volumes, and their capabilities. The discovered storage is normalized into a
   * common format with capabilities and metrics.
   *
   * @param providerId unique identifier for the provider instance
   * @param connectionInfo provider-specific connection details (URI, credentials, etc.)
   * @return future with list of discovered storage entries
   */
  CompletableFuture<List<DiscoveredStorage>> discoverStorage(
      UUID providerId, Map<String, Object> connectionInfo);

  /**
   * Test connectivity to the provider.
   *
   * <p>Validates that the provider can be reached with the given connection information and that
   * authentication is successful. This is used during provider registration to verify configuration
   * before saving.
   *
   * @param connectionInfo provider-specific connection details
   * @return future with test result indicating success or failure with error message
   */
  CompletableFuture<StorageDiscoveryTestResult> testConnection(Map<String, Object> connectionInfo);

  /**
   * Get the capabilities of this storage discovery provider.
   *
   * <p>Returns information about what types of storage this provider can discover, what metrics are
   * available, and what features are supported.
   *
   * @return discovery capabilities
   */
  StorageDiscoveryCapabilities getCapabilities();

  /** Represents a discovered storage entry from a provider. */
  record DiscoveredStorage(
      String externalId,
      String name,
      String storageType,
      Map<String, Object> capabilities,
      Map<String, Object> metrics,
      String nodeId) {}

  /** Result of a storage discovery connection test. */
  record StorageDiscoveryTestResult(
      boolean success, String message, Map<String, Object> discoveredInfo) {
    public static StorageDiscoveryTestResult success(String message, Map<String, Object> info) {
      return new StorageDiscoveryTestResult(true, message, info);
    }

    public static StorageDiscoveryTestResult failure(String message) {
      return new StorageDiscoveryTestResult(false, message, Map.of());
    }
  }

  /** Capabilities of a storage discovery provider. */
  record StorageDiscoveryCapabilities(
      List<String> supportedStorageTypes,
      boolean supportsAutoDiscovery,
      boolean supportsMetrics,
      boolean supportsMultiNode,
      Map<String, String> requiredConnectionFields) {}
}
