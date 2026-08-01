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

/** Request for VM operation (start, stop, restart, suspend, resume) */
public record VmOperationRequest(
    UUID vmId, String externalVmId, Map<String, String> parameters, String correlationId) {
  public static VmOperationRequestBuilder builder() {
    return new VmOperationRequestBuilder();
  }

  public static class VmOperationRequestBuilder {
    private UUID vmId;
    private String externalVmId;
    private Map<String, String> parameters;
    private String correlationId;

    public VmOperationRequestBuilder vmId(UUID vmId) {
      this.vmId = vmId;
      return this;
    }

    public VmOperationRequestBuilder externalVmId(String externalVmId) {
      this.externalVmId = externalVmId;
      return this;
    }

    public VmOperationRequestBuilder parameters(Map<String, String> parameters) {
      this.parameters = parameters;
      return this;
    }

    public VmOperationRequestBuilder correlationId(String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    public VmOperationRequest build() {
      return new VmOperationRequest(vmId, externalVmId, parameters, correlationId);
    }
  }
}
