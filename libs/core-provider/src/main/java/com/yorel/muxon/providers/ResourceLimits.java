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

/** Resource limits */
public record ResourceLimits(int maxCpuCores, int maxMemoryMb, int maxStorageGb, int maxVms) {
  public static ResourceLimitsBuilder builder() {
    return new ResourceLimitsBuilder();
  }

  public static class ResourceLimitsBuilder {
    private Integer maxCpuCores;
    private Integer maxMemoryMb;
    private Integer maxStorageGb;
    private Integer maxVms;

    public ResourceLimitsBuilder maxCpuCores(Integer maxCpuCores) {
      this.maxCpuCores = maxCpuCores;
      return this;
    }

    public ResourceLimitsBuilder maxMemoryMb(Integer maxMemoryMb) {
      this.maxMemoryMb = maxMemoryMb;
      return this;
    }

    public ResourceLimitsBuilder maxStorageGb(Integer maxStorageGb) {
      this.maxStorageGb = maxStorageGb;
      return this;
    }

    public ResourceLimitsBuilder maxVms(Integer maxVms) {
      this.maxVms = maxVms;
      return this;
    }

    public ResourceLimits build() {
      return new ResourceLimits(
          maxCpuCores != null ? maxCpuCores : 0,
          maxMemoryMb != null ? maxMemoryMb : 0,
          maxStorageGb != null ? maxStorageGb : 0,
          maxVms != null ? maxVms : 0);
    }
  }
}
