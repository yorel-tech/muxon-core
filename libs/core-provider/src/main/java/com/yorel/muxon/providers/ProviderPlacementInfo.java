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

/** Provider-specific placement information */
public record ProviderPlacementInfo(
    Reference targetResource, // Reference with name + external ID
    Map<String, String> preferences, // storage pool, network bridge, etc.
    Map<String, String> constraints // affinity rules, resource limits, etc.
    ) {
  public static ProviderPlacementInfoBuilder builder() {
    return new ProviderPlacementInfoBuilder();
  }

  public static class ProviderPlacementInfoBuilder {
    private Reference targetResource;
    private Map<String, String> preferences;
    private Map<String, String> constraints;

    public ProviderPlacementInfoBuilder targetResource(Reference targetResource) {
      this.targetResource = targetResource;
      return this;
    }

    public ProviderPlacementInfoBuilder preferences(Map<String, String> preferences) {
      this.preferences = preferences;
      return this;
    }

    public ProviderPlacementInfoBuilder constraints(Map<String, String> constraints) {
      this.constraints = constraints;
      return this;
    }

    public ProviderPlacementInfo build() {
      return new ProviderPlacementInfo(targetResource, preferences, constraints);
    }
  }
}
