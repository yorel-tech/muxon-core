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

import com.yorel.muxon.db.model.RouteTableEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.RouteTableEntryRepository;
import com.yorel.muxon.db.repository.RouteTableRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RouteTablesService {

  @Autowired private RouteTableRepository routeTableRepository;

  @Autowired private RouteTableEntryRepository routeTableEntryRepository;

  @Autowired private VpcRepository vpcRepository;

  public List<RouteTableEntity> listByVpc(UUID vpcId) {
    return routeTableRepository.findByVpcId(vpcId);
  }

  @Transactional
  public RouteTableEntity create(UUID vpcId, String name) {
    VpcEntity vpc =
        vpcRepository
            .findById(vpcId)
            .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));
    RouteTableEntity entity = new RouteTableEntity();
    entity.setVpc(vpc);
    entity.setName(name);
    entity.setMain(false);
    return routeTableRepository.save(entity);
  }

  public RouteTableEntity getById(UUID routeTableId) {
    return routeTableRepository
        .findById(routeTableId)
        .orElseThrow(() -> new EntityNotFoundException("Route table not found: " + routeTableId));
  }

  @Transactional
  public void delete(UUID routeTableId) {
    RouteTableEntity entity = getById(routeTableId);
    if (entity.isMain()) {
      throw new IllegalStateException("CONFLICT: Cannot delete the main route table");
    }
    routeTableRepository.deleteById(routeTableId);
  }

  public List<RouteTableEntryEntity> listEntries(UUID routeTableId) {
    return routeTableEntryRepository.findByRouteTableId(routeTableId);
  }

  @Transactional
  public RouteTableEntryEntity addEntry(
      UUID routeTableId, String destinationCidr, RouteTargetType targetType, UUID targetId) {
    RouteTableEntity rt = getById(routeTableId);
    RouteTableEntryEntity entry = new RouteTableEntryEntity();
    entry.setRouteTable(rt);
    entry.setDestinationCidr(destinationCidr);
    entry.setTargetType(targetType);
    entry.setTargetId(targetId);
    return routeTableEntryRepository.save(entry);
  }

  @Transactional
  public void deleteEntry(UUID entryId) {
    if (!routeTableEntryRepository.existsById(entryId)) {
      throw new EntityNotFoundException("Route table entry not found: " + entryId);
    }
    routeTableEntryRepository.deleteById(entryId);
  }
}
