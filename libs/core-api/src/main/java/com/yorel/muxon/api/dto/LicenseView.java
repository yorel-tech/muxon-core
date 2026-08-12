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

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** View model for exposing high-level license information via /api/v1/info. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseView {

  /** License type or edition label, e.g. "core", "enterprise". */
  private String type;

  /** Optional expiration date in ISO-8601 format (e.g. 2027-12-01). */
  private String expires;

  /**
   * Optional limits or quotas associated with the license, such as node counts or cluster limits.
   */
  private Map<String, Object> limits;
}
