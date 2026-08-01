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

import com.yorel.muxon.db.model.StackEntity;
import com.yorel.muxon.db.model.StackEntity.StackStatus;
import com.yorel.muxon.db.model.TenantDatacenterGrantEntity;
import com.yorel.muxon.db.repository.StackRepository;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StackService {

  @Autowired private StackRepository stackRepository;

  @Autowired private TenantDatacenterGrantRepository grantRepository;

  public List<StackEntity> listByGrant(UUID tenantId, UUID datacenterId) {
    TenantDatacenterGrantEntity grant = findGrant(tenantId, datacenterId);
    return stackRepository.findByTenantDatacenterGrantId(grant.getId());
  }

  @Transactional
  public StackEntity create(
      UUID tenantId,
      UUID datacenterId,
      String name,
      String description,
      Map<String, String> metadata) {
    TenantDatacenterGrantEntity grant = findGrant(tenantId, datacenterId);

    if (stackRepository.existsByTenantDatacenterGrantIdAndName(grant.getId(), name)) {
      throw new IllegalStateException(
          "CONFLICT: Stack with name '" + name + "' already exists in this grant");
    }

    StackEntity entity = new StackEntity();
    entity.setTenantDatacenterGrant(grant);
    entity.setName(name);
    entity.setDescription(description);
    entity.setMetadata(metadata);
    entity.setStatus(StackStatus.ACTIVE);
    return stackRepository.save(entity);
  }

  public StackEntity getById(UUID stackId) {
    return stackRepository
        .findById(stackId)
        .orElseThrow(() -> new EntityNotFoundException("Stack not found: " + stackId));
  }

  public StackEntity getByIdAndTenant(UUID stackId, UUID tenantId) {
    StackEntity stack = getById(stackId);
    if (!stack.getTenantDatacenterGrant().getTenant().getId().equals(tenantId)) {
      throw new EntityNotFoundException("Stack not found: " + stackId);
    }
    return stack;
  }

  @Transactional
  public StackEntity update(
      UUID stackId, String name, String description, Map<String, String> metadata) {
    StackEntity entity = getById(stackId);
    if (name != null) entity.setName(name);
    if (description != null) entity.setDescription(description);
    if (metadata != null) entity.setMetadata(metadata);
    return stackRepository.save(entity);
  }

  @Transactional
  public void delete(UUID stackId) {
    StackEntity entity = getById(stackId);
    if (stackRepository.hasActiveVms(stackId)) {
      throw new IllegalStateException(
          "CONFLICT: Stack has active VMs; delete or terminate them first");
    }
    entity.setStatus(StackStatus.DELETING);
    stackRepository.save(entity);
  }

  private TenantDatacenterGrantEntity findGrant(UUID tenantId, UUID datacenterId) {
    return grantRepository
        .findByTenant_IdAndDatacenter_Id(tenantId, datacenterId)
        .orElseThrow(
            () ->
                new EntityNotFoundException(
                    "No active grant found for tenant "
                        + tenantId
                        + " in datacenter "
                        + datacenterId));
  }
}
