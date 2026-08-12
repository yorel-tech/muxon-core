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

import com.yorel.muxon.api.model.SystemOverview;
import com.yorel.muxon.api.model.TenantOverview;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import com.yorel.muxon.db.repository.TenantRepository;
import com.yorel.muxon.db.repository.UserRoleBindingViewRepository;
import com.yorel.muxon.db.repository.VmRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {

  @Autowired private DatacenterRepository datacenterRepository;

  @Autowired private TenantRepository tenantRepository;

  @Autowired private UserRoleBindingViewRepository userRoleBindingViewRepository;

  @Autowired private VmRepository vmRepository;

  @Autowired private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

  public SystemOverview getSystemOverview() {
    UUID systemId = UUID.fromString(Constants.SYSTEM_ID);
    long dc = datacenterRepository.count();
    long tenants = tenantRepository.countExcludingId(systemId);
    long tenantUsers = userRoleBindingViewRepository.countByScopeType("TENANT");
    long vms = vmRepository.count();
    return new SystemOverview(dc, tenants, tenantUsers, vms);
  }

  public TenantOverview getTenantOverview(UUID tenantId) {
    if (!tenantRepository.existsById(tenantId)) {
      throw new EntityNotFoundException("Tenant not found: " + tenantId);
    }
    long grants = tenantDatacenterGrantRepository.countByTenant_Id(tenantId);
    long users = userRoleBindingViewRepository.countByScopeTypeAndScopeId("TENANT", tenantId);
    long vms = vmRepository.countByTenantId(tenantId);
    return new TenantOverview(grants, users, vms);
  }
}
