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
import com.yorel.muxon.db.model.DatacenterEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DatacenterRepository extends JpaRepository<DatacenterEntity, java.util.UUID> {

  /** Find a datacenter by its node cluster ID. */
  Optional<DatacenterEntity> findByNodeClusterId(UUID nodeClusterId);

  /** Check if a datacenter exists with the given name. */
  boolean existsByName(String name);

  /**
   * Find datacenters filtered by provider type. Provider type is derived from the node cluster's
   * provider.
   */
  @Query(
      "SELECT d FROM DatacenterEntity d JOIN d.nodeCluster nc JOIN nc.provider p WHERE p.type = :providerType")
  java.util.List<DatacenterEntity> findByProviderType(
      @Param("providerType") ProviderType providerType);

  @Query(
      "SELECT d FROM DatacenterEntity d JOIN FETCH d.nodeCluster nc JOIN FETCH nc.provider WHERE d.id = :id")
  Optional<DatacenterEntity> findByIdWithNodeClusterAndProvider(@Param("id") UUID id);
}
