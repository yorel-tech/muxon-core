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

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Top-level response model for the /api/v1/info endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoResponse {

  /** Product name, e.g. "Muxon". */
  private String product;

  /** Edition identifier, serialized as a simple string (e.g. "core", "enterprise"). */
  private String edition;

  /** Product version string. */
  private String version;

  /** High-level license summary. */
  private LicenseView license;

  /** Unified capability set represented as string identifiers. */
  private List<String> capabilities;

  /** List of backend modules currently available (compute, storage, network, etc). */
  private List<ModuleView> modules;

  /** Optional additional attributes for convenience flags (e.g. multipleDatacentersPerTenant). */
  private Map<String, Object> extras;
}
