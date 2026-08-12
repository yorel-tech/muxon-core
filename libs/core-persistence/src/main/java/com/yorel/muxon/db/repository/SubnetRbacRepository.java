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

import com.yorel.muxon.db.model.SubnetRbacEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubnetRbacRepository extends JpaRepository<SubnetRbacEntity, UUID> {

  List<SubnetRbacEntity> findBySubnetId(UUID subnetId);

  Optional<SubnetRbacEntity> findBySubnetIdAndPrincipalTypeAndPrincipalId(
      UUID subnetId, SubnetPrincipalType principalType, UUID principalId);

  @Query(
      "SELECT COUNT(r) > 0 FROM SubnetRbacEntity r WHERE r.subnet.id = :subnetId "
          + "AND r.principalId = :principalId AND :permission MEMBER OF r.permissions")
  boolean hasPermission(
      @Param("subnetId") UUID subnetId,
      @Param("principalId") UUID principalId,
      @Param("permission") String permission);

  @Query(
      "SELECT DISTINCT s.subnet.id FROM SubnetRbacEntity s WHERE s.principalId = :principalId "
          + "AND s.subnet.vpc.id = :vpcId "
          + "AND (s.permissions IS NOT EMPTY)")
  List<UUID> findAuthorizedSubnetIds(
      @Param("principalId") UUID principalId, @Param("vpcId") UUID vpcId);
}
