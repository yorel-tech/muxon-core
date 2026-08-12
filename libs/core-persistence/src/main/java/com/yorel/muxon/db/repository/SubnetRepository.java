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

import com.yorel.muxon.db.model.SubnetEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubnetRepository extends JpaRepository<SubnetEntity, UUID> {

  List<SubnetEntity> findByVpcId(UUID vpcId);

  List<SubnetEntity> findByVpcIdAndDatacenterId(UUID vpcId, UUID datacenterId);

  long countByVpcId(UUID vpcId);

  /**
   * Check for CIDR overlap within the same VPC (excluding a specific subnet by ID if updating).
   * Uses PostgreSQL inet operators via a native query.
   */
  @Query(
      value =
          "SELECT COUNT(*) > 0 FROM subnet s WHERE s.vpc_id = :vpcId AND s.id != :excludeId "
              + "AND (inet(s.cidr) >>= inet(:cidr) OR inet(:cidr) >>= inet(s.cidr))",
      nativeQuery = true)
  boolean existsOverlappingCidrInVpc(
      @Param("vpcId") UUID vpcId, @Param("cidr") String cidr, @Param("excludeId") UUID excludeId);
}
