package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.TenantsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.services.TenantsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class TenantsController implements TenantsApi {

    @Autowired
    private TenantsService tenantsService;

    public ResponseEntity<Tenant> createTenant(TenantCreate tenantCreate) {
        Tenant tenant = tenantsService.createTenant(tenantCreate);
        return ResponseEntity.status(201).body(tenant);
    }

    @Override
    public ResponseEntity<Void> deleteTenant(UUID tenantId) {
        tenantsService.deleteTenant(tenantId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Tenant> getTenant(UUID tenantId) {
        Tenant tenant = tenantsService.getTenant(tenantId);
        return ResponseEntity.ok(tenant);
    }

    @Override
    public ResponseEntity<TenantList> listTenants(Integer page, Integer perPage, String sort, String name,
            String status) {
        TenantList tenantList = tenantsService.listTenants(page, perPage, sort, name, status);
        return ResponseEntity.ok(tenantList);
    }

    @Override
    public ResponseEntity<Tenant> patchTenant(UUID tenantId, TenantUpdate tenantUpdate) {
        Tenant tenant = tenantsService.patchTenant(tenantId, tenantUpdate);
        return ResponseEntity.ok(tenant);
    }

    @Override
    public ResponseEntity<Tenant> updateTenant(UUID tenantId, TenantUpdate tenantUpdate) {
        Tenant tenant = tenantsService.updateTenant(tenantId, tenantUpdate);
        return ResponseEntity.ok(tenant);
    }
}
