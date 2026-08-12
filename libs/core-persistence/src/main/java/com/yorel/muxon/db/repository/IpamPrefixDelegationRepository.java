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

import com.yorel.muxon.db.model.IpamPrefixDelegationEntity;
import com.yorel.muxon.db.model.IpamPrefixDelegationEntity.IpamDelegationStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IpamPrefixDelegationRepository
    extends JpaRepository<IpamPrefixDelegationEntity, UUID> {

  List<IpamPrefixDelegationEntity> findBySubnetId(UUID subnetId);

  List<IpamPrefixDelegationEntity> findBySubnetIdAndStatus(
      UUID subnetId, IpamDelegationStatus status);

  List<IpamPrefixDelegationEntity> findByStatus(IpamDelegationStatus status);

  /** Check for CIDR overlap in active delegations within the same subnet. */
  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM ipam_prefix_delegation d "
              + "WHERE d.subnet_id = :subnetId AND d.id != :excludeId AND d.status IN ('PENDING', 'ACTIVE') "
              + "AND (inet(d.delegated_cidr) >>= inet(:cidr) OR inet(:cidr) >>= inet(d.delegated_cidr))",
      nativeQuery = true)
  boolean existsOverlappingDelegation(
      @Param("subnetId") UUID subnetId,
      @Param("cidr") String cidr,
      @Param("excludeId") UUID excludeId);
}
