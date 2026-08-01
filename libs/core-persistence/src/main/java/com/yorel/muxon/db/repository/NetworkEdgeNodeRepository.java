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

import com.yorel.muxon.db.model.NetworkEdgeNodeEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeStatus;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkEdgeNodeRepository extends JpaRepository<NetworkEdgeNodeEntity, UUID> {

  List<NetworkEdgeNodeEntity> findByDatacenterId(UUID datacenterId);

  List<NetworkEdgeNodeEntity> findByDatacenterIdAndStatus(
      UUID datacenterId, NetworkEdgeNodeStatus status);

  Optional<NetworkEdgeNodeEntity> findFirstByDatacenterIdAndStatus(
      UUID datacenterId, NetworkEdgeNodeStatus status);

  boolean existsByDatacenterIdAndType(UUID datacenterId, NetworkEdgeNodeType type);
}
