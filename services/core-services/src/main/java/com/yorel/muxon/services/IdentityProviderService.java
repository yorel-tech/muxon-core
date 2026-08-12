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
package com.yorel.muxon.services;

import com.yorel.muxon.common.Constants;
import com.yorel.muxon.db.model.IdentityProviderEntity;
import com.yorel.muxon.db.repository.IdentityProviderRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdentityProviderService {

  @Autowired private IdentityProviderRepository idpRepository;

  /** Return the default identity provider (all tenants share unless overridden per tenant). */
  public Optional<IdentityProviderEntity> getDefaultProvider() {
    return idpRepository.findById(UUID.fromString(Constants.DEFAULT_IDP_ID));
  }

  /**
   * Return provider for the given tenant. If tenant has no specific provider configured, falls back
   * to default provider.
   */
  public Optional<IdentityProviderEntity> findByTenantId(String tenantId) {
    if (tenantId == null || tenantId.isBlank()) {
      return getDefaultProvider();
    }
    // TODO: per-tenant IDP configuration when supported (e.g. tenant_id column on
    // identity_provider)
    return getDefaultProvider();
  }

  /**
   * Return system-level identity provider (is_system = true). All tenants share this single OIDC
   * provider.
   */
  public Optional<IdentityProviderEntity> getSystemProvider() {
    List<IdentityProviderEntity> systemProviders = idpRepository.findSystemProvider();
    return systemProviders.isEmpty() ? Optional.empty() : Optional.of(systemProviders.get(0));
  }
}
