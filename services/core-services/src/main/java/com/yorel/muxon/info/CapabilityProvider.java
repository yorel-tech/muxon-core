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
package com.yorel.muxon.info;

import java.util.Set;

/**
 * Contract for components that contribute capabilities to the unified capability set exposed via
 * the /api/v1/info endpoint.
 */
public interface CapabilityProvider {

  /**
   * Return the set of capabilities contributed by this provider.
   *
   * @return a set of capability strings
   */
  Set<String> getCapabilities();
}
