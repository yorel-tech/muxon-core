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
package com.yorel.muxon.services.storage;

import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.model.StorageClassEntity;
import com.yorel.muxon.services.storage.scheduler.CapabilityFilter;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Capability-based compatibility between provider storage and a storage class for mapping UX.
 *
 * <p>Provider capabilities must be a subset of the class capability map: every class key must be
 * satisfied by the provider, and the provider must not advertise keys absent from the class. Uses
 * the same per-value rules as {@link CapabilityFilter} ({@code any} / {@code *} on the class side).
 * {@link CapabilityFilter#meetsConstraintsForListing} is applied for IOPS / latency-style
 * constraints (volume size treated as 0 so free-space is not required).
 */
@Component
public class ProviderStorageClassMappingEvaluator {

  private final CapabilityFilter capabilityFilter;

  public ProviderStorageClassMappingEvaluator(CapabilityFilter capabilityFilter) {
    this.capabilityFilter = capabilityFilter;
  }

  /**
   * True if this provider storage is mapped to the class by capability rules (excluding overrides).
   */
  public boolean matchesByCapabilities(
      StorageClassEntity storageClass, ProviderStorageEntity storage) {
    Map<String, Object> required = storageClass.getCapabilities();
    Map<String, Object> actual = storage.getCapabilities();
    if (required == null || required.isEmpty()) {
      return false;
    }
    if (actual == null || actual.isEmpty()) {
      return false;
    }
    for (Map.Entry<String, Object> entry : required.entrySet()) {
      if (!matchesValue(entry.getValue(), actual.get(entry.getKey()))) {
        return false;
      }
    }
    for (String providerKey : actual.keySet()) {
      if (!required.containsKey(providerKey)) {
        return false;
      }
      if (!matchesValue(required.get(providerKey), actual.get(providerKey))) {
        return false;
      }
    }
    return capabilityFilter.meetsConstraintsForListing(storage, storageClass);
  }

  private static boolean matchesValue(Object required, Object actual) {
    if (required == null) {
      return true;
    }
    if (actual == null) {
      return false;
    }
    if ("any".equals(String.valueOf(required)) || "*".equals(String.valueOf(required))) {
      return true;
    }
    return String.valueOf(required).equals(String.valueOf(actual));
  }
}
