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

import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity;
import com.yorel.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import com.yorel.muxon.db.repository.SubnetRbacRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubnetRbacService {

  @Autowired private SubnetRbacRepository rbacRepository;

  @Autowired private SubnetRepository subnetRepository;

  public List<SubnetRbacEntity> listBySubnet(UUID subnetId) {
    return rbacRepository.findBySubnetId(subnetId);
  }

  @Transactional
  public SubnetRbacEntity create(
      UUID subnetId,
      SubnetPrincipalType principalType,
      UUID principalId,
      List<String> permissions) {
    SubnetEntity subnet =
        subnetRepository
            .findById(subnetId)
            .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

    rbacRepository
        .findBySubnetIdAndPrincipalTypeAndPrincipalId(subnetId, principalType, principalId)
        .ifPresent(
            existing -> {
              throw new IllegalStateException(
                  "CONFLICT: RBAC binding already exists for this principal on subnet " + subnetId);
            });

    SubnetRbacEntity entity = new SubnetRbacEntity();
    entity.setSubnet(subnet);
    entity.setPrincipalType(principalType);
    entity.setPrincipalId(principalId);
    entity.setPermissions(permissions);
    return rbacRepository.save(entity);
  }

  @Transactional
  public void delete(UUID rbacId) {
    if (!rbacRepository.existsById(rbacId)) {
      throw new EntityNotFoundException("RBAC binding not found: " + rbacId);
    }
    rbacRepository.deleteById(rbacId);
  }

  public boolean hasPermission(UUID subnetId, UUID principalId, String permission) {
    return rbacRepository.hasPermission(subnetId, principalId, permission);
  }

  public List<SubnetEntity> getAuthorizedSubnets(UUID tenantId, UUID vpcId, UUID stackId) {
    List<UUID> authorizedIds = rbacRepository.findAuthorizedSubnetIds(stackId, vpcId);
    return subnetRepository.findByVpcId(vpcId).stream()
        .filter(s -> authorizedIds.contains(s.getId()))
        .toList();
  }
}
