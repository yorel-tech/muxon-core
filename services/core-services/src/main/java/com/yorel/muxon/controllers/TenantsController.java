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

import com.yorel.muxon.api.TenantsApi;
import com.yorel.muxon.api.model.Tenant;
import com.yorel.muxon.api.model.TenantCreate;
import com.yorel.muxon.api.model.TenantList;
import com.yorel.muxon.api.model.TenantSettings;
import com.yorel.muxon.api.model.TenantUpdate;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.auth.UserPrincipal;
import com.yorel.muxon.services.TenantsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantsController implements TenantsApi {

  @Autowired private TenantsService tenantsService;

  @Override
  @RequiresPermission(Permission.TENANT_MANAGE)
  public ResponseEntity<Tenant> createTenant(@Valid @RequestBody TenantCreate tenantCreate) {
    Tenant tenant = tenantsService.createTenant(tenantCreate);
    return ResponseEntity.status(201).body(tenant);
  }

  @Override
  @RequiresPermission(Permission.TENANT_MANAGE)
  public ResponseEntity<Void> deleteTenant(@NotNull @PathVariable("tenantId") UUID tenantId) {
    tenantsService.deleteTenant(tenantId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Tenant> getCurrentTenant(String slug) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
      return ResponseEntity.status(401).build();
    }
    Tenant tenant = tenantsService.getCurrentTenantForUser(user.id(), slug);
    return ResponseEntity.ok(tenant);
  }

  @Override
  public ResponseEntity<TenantList> getMyTenants() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) {
      return ResponseEntity.status(401).build();
    }
    TenantList tenants = tenantsService.getTenantsForCurrentUser(user.id());
    return ResponseEntity.ok(tenants);
  }

  @Override
  @RequiresPermission(Permission.TENANT_READ)
  public ResponseEntity<Tenant> getTenant(@NotNull @PathVariable("tenantId") UUID tenantId) {
    Tenant tenant = tenantsService.getTenant(tenantId);
    return ResponseEntity.ok(tenant);
  }

  @Override
  @RequiresPermission(Permission.TENANT_READ)
  public ResponseEntity<TenantList> listTenants(
      @RequestParam(value = "page", required = false, defaultValue = "1") Integer page,
      @RequestParam(value = "perPage", required = false, defaultValue = "20") Integer perPage,
      @RequestParam(value = "sort", required = false) String sort,
      @RequestParam(value = "name", required = false) String name,
      @RequestParam(value = "status", required = false) String status) {
    TenantList tenantList = tenantsService.listTenants(page, perPage, sort, name, status);
    return ResponseEntity.ok(tenantList);
  }

  @Override
  @RequiresPermission(Permission.TENANT_EDIT)
  public ResponseEntity<Tenant> patchTenant(
      @NotNull @PathVariable("tenantId") UUID tenantId,
      @Valid @RequestBody TenantUpdate tenantUpdate) {
    Tenant tenant = tenantsService.patchTenant(tenantId, tenantUpdate);
    return ResponseEntity.ok(tenant);
  }

  @Override
  @RequiresPermission(Permission.TENANT_EDIT)
  public ResponseEntity<Tenant> updateTenant(
      @NotNull @PathVariable("tenantId") UUID tenantId,
      @Valid @RequestBody TenantUpdate tenantUpdate) {
    Tenant tenant = tenantsService.updateTenant(tenantId, tenantUpdate);
    return ResponseEntity.ok(tenant);
  }

  @Override
  @RequiresPermission(Permission.TENANT_READ_SETTINGS)
  public ResponseEntity<TenantSettings> getTenantSettings(
      @NotNull @PathVariable("tenantId") UUID tenantId) {
    TenantSettings settings = tenantsService.getTenantSettings(tenantId);
    return ResponseEntity.ok(settings);
  }

  @Override
  @RequiresPermission(Permission.TENANT_MANAGE)
  public ResponseEntity<TenantSettings> replaceTenantSettings(
      @NotNull @PathVariable("tenantId") UUID tenantId,
      @Valid @RequestBody TenantSettings tenantSettings) {
    TenantSettings settings = tenantsService.replaceTenantSettings(tenantId, tenantSettings);
    return ResponseEntity.ok(settings);
  }

  @Override
  @RequiresPermission(Permission.TENANT_MANAGE)
  public ResponseEntity<TenantSettings> updateTenantSettings(
      @NotNull @PathVariable("tenantId") UUID tenantId,
      @Valid @RequestBody TenantSettings tenantSettings) {
    TenantSettings settings = tenantsService.updateTenantSettings(tenantId, tenantSettings);
    return ResponseEntity.ok(settings);
  }
}
