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
package com.yorel.muxon.api.dto;

import com.yorel.muxon.api.enums.BootstrapStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for bootstrap status response. Returns only the bootstrap status enum, not detailed
 * configuration data. This is used by the public /status endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BootstrapStatusDto {

  /**
   * Current bootstrap status indicating the system initialization state. NOTREADY - No bootstrap
   * configuration found BOOTSTRAPPED - Bootstrap data has been inserted (IDP, system admin, tenant)
   * READY - All required setup steps have been completed
   */
  private BootstrapStatus systemStatus;

  /**
   * Creates a BootstrapStatusDto with the specified system status.
   *
   * @param systemStatus the bootstrap status
   * @return a new BootstrapStatusDto instance
   */
  public static BootstrapStatusDto of(BootstrapStatus systemStatus) {
    return new BootstrapStatusDto(systemStatus);
  }
}
