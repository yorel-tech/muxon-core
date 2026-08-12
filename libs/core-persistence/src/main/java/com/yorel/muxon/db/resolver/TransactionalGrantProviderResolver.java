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
package com.yorel.muxon.db.resolver;

import com.yorel.muxon.providers.TenantDatacenterGrantResolver;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs {@link TenantDatacenterGrantResolver} inside a read transaction so lazy associations (grant
 * → datacenter → node cluster → provider) initialize correctly. Used from services whose
 * entrypoints are not transactional (e.g. orchestrator {@code VmTaskExecutor} self-invocations).
 */
@Service
public class TransactionalGrantProviderResolver {

  @Autowired private TenantDatacenterGrantResolver tenantDatacenterGrantResolver;

  @Transactional(readOnly = true)
  public Optional<String> resolveProviderId(UUID tenantDatacenterGrantId) {
    return tenantDatacenterGrantResolver.resolveProviderId(tenantDatacenterGrantId);
  }
}
