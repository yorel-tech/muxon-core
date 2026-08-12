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
package com.yorel.muxon.controllers;

import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.services.storage.ProviderStorageDiscoveryService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for provider storage operations.
 *
 * <p>Provides endpoints to view discovered provider storage and trigger storage sync.
 */
@RestController
@RequestMapping("/api/v1/provider-storage")
public class ProviderStorageController {

  private static final Logger log = LoggerFactory.getLogger(ProviderStorageController.class);

  private final ProviderStorageDiscoveryService discoveryService;

  public ProviderStorageController(ProviderStorageDiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  /**
   * Get all storage for a specific provider.
   *
   * @param providerId provider ID
   * @return list of provider storage
   */
  @GetMapping("/provider/{providerId}")
  public ResponseEntity<List<ProviderStorageEntity>> getStorageByProvider(
      @PathVariable UUID providerId) {
    log.debug("Getting storage for provider {}", providerId);
    List<ProviderStorageEntity> storage = discoveryService.getProviderStorage(providerId);
    return ResponseEntity.ok(storage);
  }

  /**
   * Get enabled storage for a specific provider.
   *
   * @param providerId provider ID
   * @return list of enabled provider storage
   */
  @GetMapping("/provider/{providerId}/enabled")
  public ResponseEntity<List<ProviderStorageEntity>> getEnabledStorageByProvider(
      @PathVariable UUID providerId) {
    log.debug("Getting enabled storage for provider {}", providerId);
    List<ProviderStorageEntity> storage = discoveryService.getEnabledProviderStorage(providerId);
    return ResponseEntity.ok(storage);
  }

  /** Trigger storage discovery and sync for a provider (async; returns task id). */
  @PostMapping("/provider/{providerId}/sync")
  public ResponseEntity<Map<String, Object>> syncProviderStorage(@PathVariable UUID providerId) {
    log.info("Triggering storage sync for provider {}", providerId);

    try {
      UUID taskId = discoveryService.enqueueStorageDiscovery(providerId);
      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("providerId", providerId);
      result.put("taskId", taskId);
      result.put("message", "Poll GET /api/v1/tasks/" + taskId);
      return ResponseEntity.status(202).body(result);
    } catch (IllegalArgumentException e) {
      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("providerId", providerId);
      result.put("error", e.getMessage());
      return ResponseEntity.badRequest().body(result);
    } catch (Exception e) {
      log.error(
          "Failed to enqueue storage sync for provider {}: {}", providerId, e.getMessage(), e);
      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("providerId", providerId);
      result.put("error", e.getMessage());
      return ResponseEntity.status(500).body(result);
    }
  }

  /** Trigger storage discovery for all supported providers (async; one task per provider). */
  @PostMapping("/sync-all")
  public ResponseEntity<Map<String, Object>> syncAllProviders() {
    log.info("Triggering storage sync for all providers");

    try {
      Map<UUID, UUID> tasks = discoveryService.enqueueStorageDiscoveryForAllProviders();
      Map<String, Object> serializable = new LinkedHashMap<>();
      tasks.forEach(
          (pid, tid) -> serializable.put(pid.toString(), tid != null ? tid.toString() : null));

      Map<String, Object> result = new HashMap<>();
      result.put("success", true);
      result.put("providerCount", tasks.size());
      result.put("tasksByProviderId", serializable);
      result.put("message", "Poll each task via GET /api/v1/tasks/{taskId}");
      return ResponseEntity.status(202).body(result);
    } catch (Exception e) {
      log.error("Failed to enqueue sync for all providers: {}", e.getMessage(), e);
      Map<String, Object> result = new HashMap<>();
      result.put("success", false);
      result.put("error", e.getMessage());
      return ResponseEntity.status(500).body(result);
    }
  }
}
