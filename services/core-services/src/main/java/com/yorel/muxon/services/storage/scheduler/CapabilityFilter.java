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
package com.yorel.muxon.services.storage.scheduler;

import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Filters provider storage based on capability matching.
 *
 * <p>Eliminates storage that doesn't meet the required capabilities and constraints.
 */
@Component
public class CapabilityFilter {

  private static final Logger log = LoggerFactory.getLogger(CapabilityFilter.class);

  /**
   * Filter provider storage by capabilities and constraints.
   *
   * @param candidates list of candidate provider storage
   * @param context scheduler context with requirements
   * @return filtered list of matching provider storage
   */
  public List<ProviderStorageEntity> filter(
      List<ProviderStorageEntity> candidates, SchedulerContext context) {

    List<ProviderStorageEntity> filtered = new ArrayList<>();

    for (ProviderStorageEntity storage : candidates) {
      if (matchesCapabilities(storage, context) && meetsConstraints(storage, context)) {
        filtered.add(storage);
      }
    }

    log.debug(
        "Filtered {} candidates to {} matching storage entries",
        candidates.size(),
        filtered.size());

    return filtered;
  }

  /**
   * Constraint checks for admin mapping / listing (no minimum volume size; free space not enforced
   * when size is 0).
   */
  public boolean meetsConstraintsForListing(
      ProviderStorageEntity storage, StorageClassEntity storageClass) {
    SchedulerContext ctx =
        SchedulerContext.builder()
            .storageClassName(storageClass.getName())
            .capabilities(storageClass.getCapabilities())
            .constraints(storageClass.getConstraints())
            .sizeBytes(0L)
            .build();
    return meetsConstraints(storage, ctx);
  }

  /** Check if storage matches required capabilities. */
  private boolean matchesCapabilities(ProviderStorageEntity storage, SchedulerContext context) {
    Map<String, Object> required = context.getCapabilities();
    Map<String, Object> actual = storage.getCapabilities();

    if (required == null || required.isEmpty()) {
      return true;
    }

    if (actual == null || actual.isEmpty()) {
      return false;
    }

    for (Map.Entry<String, Object> entry : required.entrySet()) {
      String capability = entry.getKey();
      Object requiredValue = entry.getValue();
      Object actualValue = actual.get(capability);

      if (!matchesValue(requiredValue, actualValue)) {
        log.debug(
            "Storage {} does not match capability {}: required={}, actual={}",
            storage.getName(),
            capability,
            requiredValue,
            actualValue);
        return false;
      }
    }

    return true;
  }

  /** Check if storage meets constraints. */
  private boolean meetsConstraints(ProviderStorageEntity storage, SchedulerContext context) {
    Map<String, Object> constraints = context.getConstraints();
    Map<String, Object> metrics = storage.getMetrics();

    if (constraints == null || constraints.isEmpty()) {
      return true;
    }

    if (metrics == null) {
      return false;
    }

    // Check min_iops constraint
    if (constraints.containsKey("min_iops")) {
      int minIops = ((Number) constraints.get("min_iops")).intValue();
      Object iopsObj = metrics.get("estimated_iops");
      if (iopsObj == null) {
        return false;
      }
      int actualIops = ((Number) iopsObj).intValue();
      if (actualIops < minIops) {
        log.debug(
            "Storage {} does not meet min_iops: required={}, actual={}",
            storage.getName(),
            minIops,
            actualIops);
        return false;
      }
    }

    // Check capacity constraint
    long sizeGb = context.getSizeBytes() / (1024L * 1024L * 1024L);
    Object freeGbObj = metrics.get("free_gb");
    if (freeGbObj != null) {
      long freeGb = ((Number) freeGbObj).longValue();
      if (freeGb < sizeGb) {
        log.debug(
            "Storage {} does not have enough capacity: required={}GB, available={}GB",
            storage.getName(),
            sizeGb,
            freeGb);
        return false;
      }
    }

    return true;
  }

  /** Check if a value matches the requirement. */
  private boolean matchesValue(Object required, Object actual) {
    if (required == null) {
      return true;
    }

    if (actual == null) {
      return false;
    }

    // Handle "any" wildcard
    if ("any".equals(String.valueOf(required)) || "*".equals(String.valueOf(required))) {
      return true;
    }

    // String comparison
    return String.valueOf(required).equals(String.valueOf(actual));
  }
}
