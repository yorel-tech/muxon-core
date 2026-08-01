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
package com.yorel.muxon.api.enums;

/**
 * Enum representing the three-state bootstrap lifecycle.
 *
 * <p>The lifecycle progresses as follows:
 *
 * <ul>
 *   <li>NOTREADY - Initial state, no bootstrap configuration found
 *   <li>BOOTSTRAPPED - Bootstrap data has been inserted (IDP, system admin, tenant)
 *   <li>READY - All required setup steps have been completed via the setup wizard
 * </ul>
 */
public enum BootstrapStatus {
  /**
   * Initial state - no bootstrap configuration has been performed. The system is waiting for
   * bootstrap initialization.
   */
  NOTREADY,

  /**
   * Bootstrap has been performed via muxon-initializer. IDP (with is_system=true), system admin
   * users, and tenant have been created. Setup wizard can now proceed to complete remaining steps.
   */
  BOOTSTRAPPED,

  /**
   * All required setup steps have been completed. The system is ready for normal operation. The
   * "skip to dashboard" button is enabled.
   */
  READY
}
