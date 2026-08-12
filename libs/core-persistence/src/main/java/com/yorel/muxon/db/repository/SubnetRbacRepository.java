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

  // permissions is PG TEXT[] (basic array); JPQL MEMBER OF / IS NOT EMPTY need a plural path.
  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT 1 FROM subnet_rbac r "
              + "WHERE r.subnet_id = :subnetId "
              + "AND r.principal_id = :principalId "
              + "AND :permission = ANY(r.permissions)"
              + ")",
      nativeQuery = true)
  boolean hasPermission(
      @Param("subnetId") UUID subnetId,
      @Param("principalId") UUID principalId,
      @Param("permission") String permission);

  @Query(
      value =
          "SELECT DISTINCT r.subnet_id FROM subnet_rbac r "
              + "INNER JOIN subnet s ON s.id = r.subnet_id "
              + "WHERE r.principal_id = :principalId "
              + "AND s.vpc_id = :vpcId "
              + "AND cardinality(r.permissions) > 0",
      nativeQuery = true)
  List<UUID> findAuthorizedSubnetIds(
      @Param("principalId") UUID principalId, @Param("vpcId") UUID vpcId);
}
