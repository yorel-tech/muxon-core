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

import java.util.UUID;

/** Request for VM deletion */
public record VmDeletionRequest(UUID vmId, String correlationId) {
  public static VmDeletionRequestBuilder builder() {
    return new VmDeletionRequestBuilder();
  }

  public static class VmDeletionRequestBuilder {
    private UUID vmId;
    private String correlationId;

    public VmDeletionRequestBuilder vmId(UUID vmId) {
      this.vmId = vmId;
      return this;
    }

    public VmDeletionRequestBuilder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public VmDeletionRequest build() {
      return new VmDeletionRequest(vmId, correlationId);
    }
  }
}
