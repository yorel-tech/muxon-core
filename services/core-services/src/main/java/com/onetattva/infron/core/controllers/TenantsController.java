package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.TenantsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.core.services.TenantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@RestController
public class TenantsController implements TenantsApi {

    @Autowired
    private TenantsService tenantsService;

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
    public ResponseEntity<Tenant> patchTenant(@NotNull @PathVariable("tenantId") UUID tenantId, @Valid @RequestBody TenantUpdate tenantUpdate) {
        Tenant tenant = tenantsService.patchTenant(tenantId, tenantUpdate);
        return ResponseEntity.ok(tenant);
    }

    @Override
    @RequiresPermission(Permission.TENANT_EDIT)
    public ResponseEntity<Tenant> updateTenant(@NotNull @PathVariable("tenantId") UUID tenantId, @Valid @RequestBody TenantUpdate tenantUpdate) {
        Tenant tenant = tenantsService.updateTenant(tenantId, tenantUpdate);
        return ResponseEntity.ok(tenant);
    }

    @Override
    @RequiresPermission(Permission.TENANT_READ_SETTINGS)
    public ResponseEntity<TenantSettings> getTenantSettings(@NotNull @PathVariable("tenantId") UUID tenantId) {
        TenantSettings settings = tenantsService.getTenantSettings(tenantId);
        return ResponseEntity.ok(settings);
    }

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<TenantSettings> replaceTenantSettings(@NotNull @PathVariable("tenantId") UUID tenantId, @Valid @RequestBody TenantSettings tenantSettings) {
        TenantSettings settings = tenantsService.replaceTenantSettings(tenantId, tenantSettings);
        return ResponseEntity.ok(settings);
    }

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<TenantSettings> updateTenantSettings(@NotNull @PathVariable("tenantId") UUID tenantId, @Valid @RequestBody TenantSettings tenantSettings) {
        TenantSettings settings = tenantsService.updateTenantSettings(tenantId, tenantSettings);
        return ResponseEntity.ok(settings);
    }
}
