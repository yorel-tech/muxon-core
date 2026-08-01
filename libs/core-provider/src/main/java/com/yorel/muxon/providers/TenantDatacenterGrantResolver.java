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

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the VM provider ID for a tenant datacenter grant. Implementations typically load grant
 * and datacenter from the database and return the provider ID from the linked node cluster or
 * datacenter settings.
 */
public interface TenantDatacenterGrantResolver {

  /**
   * Resolve the provider ID for the given tenant datacenter grant.
   *
   * @param tenantDatacenterGrantId the grant ID
   * @return the provider ID to use for VM operations, or empty if not found or not linked
   */
  Optional<String> resolveProviderId(UUID tenantDatacenterGrantId);
}
