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

import java.util.List;
import java.util.Map;

/** Provider capabilities */
public record ProviderCapabilities(
    List<String> supportedCpuTypes,
    List<String> supportedStorageClasses,
    List<String> supportedNetworkTypes,
    List<String> supportedOsTypes,
    ResourceLimits resourceLimits,
    Map<String, Object> features) {
  public static ProviderCapabilitiesBuilder builder() {
    return new ProviderCapabilitiesBuilder();
  }

  public static class ProviderCapabilitiesBuilder {
    private List<String> supportedCpuTypes;
    private List<String> supportedStorageClasses;
    private List<String> supportedNetworkTypes;
    private List<String> supportedOsTypes;
    private ResourceLimits resourceLimits;
    private Map<String, Object> features;

    public ProviderCapabilitiesBuilder supportedCpuTypes(List<String> supportedCpuTypes) {
      this.supportedCpuTypes = supportedCpuTypes;
      return this;
    }

    public ProviderCapabilitiesBuilder supportedStorageClasses(
        List<String> supportedStorageClasses) {
      this.supportedStorageClasses = supportedStorageClasses;
      return this;
    }

    public ProviderCapabilitiesBuilder supportedNetworkTypes(List<String> supportedNetworkTypes) {
      this.supportedNetworkTypes = supportedNetworkTypes;
      return this;
    }

    public ProviderCapabilitiesBuilder supportedOsTypes(List<String> supportedOsTypes) {
      this.supportedOsTypes = supportedOsTypes;
      return this;
    }

    public ProviderCapabilitiesBuilder resourceLimits(ResourceLimits resourceLimits) {
      this.resourceLimits = resourceLimits;
      return this;
    }

    public ProviderCapabilitiesBuilder features(Map<String, Object> features) {
      this.features = features;
      return this;
    }

    public ProviderCapabilities build() {
      return new ProviderCapabilities(
          supportedCpuTypes,
          supportedStorageClasses,
          supportedNetworkTypes,
          supportedOsTypes,
          resourceLimits,
          features);
    }
  }
}
