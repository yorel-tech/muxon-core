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

import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.db.model.ProviderEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {

  /** Find providers by type */
  List<ProviderEntity> findByType(ProviderType type);

  /** Find providers by status */
  List<ProviderEntity> findByStatus(String status);

  /** Find provider by type and status */
  List<ProviderEntity> findByTypeAndStatus(ProviderType type, String status);

  /** Find provider by name (case-insensitive) */
  Optional<ProviderEntity> findByNameIgnoreCase(String name);

  /** Check if a provider with the same name exists (excluding current provider) */
  boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

  /** Minimal projection for provider connection polling. */
  @Query(
      "SELECT p.status AS status, p.metadata AS metadata, p.capabilities AS capabilities FROM ProviderEntity p WHERE p.id = :id")
  Optional<ProviderConnectionStateProjection> findConnectionStateById(@Param("id") UUID id);

  interface ProviderConnectionStateProjection {
    String getStatus();

    java.util.Map<String, String> getMetadata();

    java.util.Map<String, String> getCapabilities();
  }
}
