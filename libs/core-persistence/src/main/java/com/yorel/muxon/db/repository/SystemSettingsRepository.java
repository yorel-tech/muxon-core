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
package com.yorel.muxon.db.repository;

import com.yorel.muxon.common.Constants;
import com.yorel.muxon.db.model.SystemSettingsEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for system_settings table. Supports both system-level defaults (tenant_id = SYSTEM_ID)
 * and tenant-specific overrides (tenant_id = tenant UUID).
 */
@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, UUID> {

  /** Find system-level settings */
  Optional<SystemSettingsEntity> findByTenantId(UUID tenantId);

  /** Find system-level settings using SYSTEM_ID constant */
  default Optional<SystemSettingsEntity> findSystemSettings() {
    return findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
  }

  /** Check if tenant has settings override */
  boolean existsByTenantId(UUID tenantId);

  /** Check if system settings exist */
  default boolean systemSettingsExist() {
    return existsByTenantId(UUID.fromString(Constants.SYSTEM_ID));
  }
}
