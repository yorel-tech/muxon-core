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
package com.yorel.muxon.controllers;

import com.yorel.muxon.api.TenantDatacentersApi;
import com.yorel.muxon.api.model.GetTenantDatacenterEffective200Response;
import com.yorel.muxon.api.model.TenantDatacenterGrant;
import com.yorel.muxon.api.model.TenantDatacenterGrantCreate;
import com.yorel.muxon.api.model.TenantDatacenterGrantList;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresAnyPermission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.TenantDatacenterGrantService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantDatacentersController implements TenantDatacentersApi {

  @Autowired private TenantDatacenterGrantService tenantDatacenterGrantService;

  @Override
  @RequiresPermission(Permission.DATACENTER_MANAGE)
  public ResponseEntity<TenantDatacenterGrant> createTenantDatacenterGrant(
      UUID tenantId,
      TenantDatacenterGrantCreate tenantDatacenterGrantCreate,
      Integer page,
      Integer perPage) {
    TenantDatacenterGrant grant =
        tenantDatacenterGrantService.createTenantDatacenterGrant(
            tenantId, tenantDatacenterGrantCreate);
    return ResponseEntity.status(201).body(grant);
  }

  @Override
  @RequiresPermission(Permission.DATACENTER_MANAGE)
  public ResponseEntity<Void> deleteTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
    tenantDatacenterGrantService.deleteTenantDatacenterGrant(tenantId, datacenterId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
  public ResponseEntity<GetTenantDatacenterEffective200Response> getTenantDatacenterEffective(
      UUID tenantId, UUID datacenterId) {
    GetTenantDatacenterEffective200Response response =
        tenantDatacenterGrantService.getTenantDatacenterEffective(tenantId, datacenterId);
    return ResponseEntity.ok(response);
  }

  @Override
  @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
  public ResponseEntity<TenantDatacenterGrant> getTenantDatacenterGrant(
      UUID tenantId, UUID datacenterId) {
    TenantDatacenterGrant grant =
        tenantDatacenterGrantService.getTenantDatacenterGrant(tenantId, datacenterId);
    return ResponseEntity.ok(grant);
  }

  @Override
  @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
  public ResponseEntity<TenantDatacenterGrantList> listTenantDatacenters(
      UUID tenantId, Integer page, Integer perPage) {
    TenantDatacenterGrantList list =
        tenantDatacenterGrantService.listTenantDatacenters(tenantId, page, perPage);
    return ResponseEntity.ok(list);
  }

  @Override
  @RequiresPermission(Permission.DATACENTER_MANAGE)
  public ResponseEntity<TenantDatacenterGrant> replaceTenantDatacenterGrant(
      UUID tenantId, UUID datacenterId, TenantDatacenterGrant tenantDatacenterGrant) {
    TenantDatacenterGrant grant =
        tenantDatacenterGrantService.replaceTenantDatacenterGrant(
            tenantId, datacenterId, tenantDatacenterGrant);
    return ResponseEntity.ok(grant);
  }

  @Override
  @RequiresPermission(Permission.DATACENTER_MANAGE)
  public ResponseEntity<TenantDatacenterGrant> updateTenantDatacenterGrant(
      UUID tenantId, UUID datacenterId, TenantDatacenterGrant tenantDatacenterGrant) {
    TenantDatacenterGrant grant =
        tenantDatacenterGrantService.updateTenantDatacenterGrant(
            tenantId, datacenterId, tenantDatacenterGrant);
    return ResponseEntity.ok(grant);
  }

  @Override
  @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
  public ResponseEntity<java.util.List<String>> getTenantStorageClasses(
      UUID tenantId, UUID datacenterId) {
    java.util.List<String> storageClasses =
        tenantDatacenterGrantService.getEffectiveStorageClasses(tenantId, datacenterId);
    return ResponseEntity.ok(storageClasses);
  }

  @Override
  @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
  public ResponseEntity<java.util.Map<String, com.yorel.muxon.api.model.StorageUsage>>
      getTenantStorageUsage(UUID tenantId, UUID datacenterId) {
    java.util.Map<String, com.yorel.muxon.api.model.StorageUsage> usage =
        tenantDatacenterGrantService.getStorageUsageByClass(tenantId, datacenterId);
    return ResponseEntity.ok(usage);
  }
}
