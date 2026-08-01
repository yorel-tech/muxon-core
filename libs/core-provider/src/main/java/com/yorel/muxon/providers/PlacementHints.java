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
package com.yorel.muxon.providers;

import java.util.Map;
import java.util.UUID;

/** Placement hints */
public record PlacementHints(UUID datacenterId, UUID nodeId, Map<String, Object> preferences) {
  public static PlacementHintsBuilder builder() {
    return new PlacementHintsBuilder();
  }

  public static class PlacementHintsBuilder {
    private UUID datacenterId;
    private UUID nodeId;
    private Map<String, Object> preferences;

    public PlacementHintsBuilder datacenterId(UUID datacenterId) {
      this.datacenterId = datacenterId;
      return this;
    }

    public PlacementHintsBuilder nodeId(UUID nodeId) {
      this.nodeId = nodeId;
      return this;
    }

    public PlacementHintsBuilder preferences(Map<String, Object> preferences) {
      this.preferences = preferences;
      return this;
    }

    public PlacementHints build() {
      return new PlacementHints(datacenterId, nodeId, preferences);
    }
  }
}
