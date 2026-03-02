package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.TenantDatacentersApi;
import com.onetattva.infron.api.model.GetTenantDatacenterEffective200Response;
import com.onetattva.infron.api.model.TenantDatacenterGrant;
import com.onetattva.infron.api.model.TenantDatacenterGrantCreate;
import com.onetattva.infron.api.model.TenantDatacenterGrantList;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.TenantDatacenterGrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TenantDatacentersController implements TenantDatacentersApi {

    @Autowired
    private TenantDatacenterGrantService tenantDatacenterGrantService;

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> createTenantDatacenterGrant(
            UUID tenantId,
            TenantDatacenterGrantCreate tenantDatacenterGrantCreate,
            Integer page,
            Integer perPage) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.createTenantDatacenterGrant(tenantId, tenantDatacenterGrantCreate);
        return ResponseEntity.status(201).body(grant);
    }

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<Void> deleteTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
        tenantDatacenterGrantService.deleteTenantDatacenterGrant(tenantId, datacenterId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.TENANT_READ)
    public ResponseEntity<GetTenantDatacenterEffective200Response> getTenantDatacenterEffective(
            UUID tenantId,
            UUID datacenterId) {
        GetTenantDatacenterEffective200Response response = tenantDatacenterGrantService.getTenantDatacenterEffective(tenantId, datacenterId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.TENANT_READ)
    public ResponseEntity<TenantDatacenterGrant> getTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.getTenantDatacenterGrant(tenantId, datacenterId);
        return ResponseEntity.ok(grant);
    }

    @Override
    @RequiresPermission(Permission.TENANT_READ)
    public ResponseEntity<TenantDatacenterGrantList> listTenantDatacenters(
            UUID tenantId,
            Integer page,
            Integer perPage) {
        TenantDatacenterGrantList list = tenantDatacenterGrantService.listTenantDatacenters(tenantId, page, perPage);
        return ResponseEntity.ok(list);
    }

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> replaceTenantDatacenterGrant(
            UUID tenantId,
            UUID datacenterId,
            TenantDatacenterGrant tenantDatacenterGrant) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.replaceTenantDatacenterGrant(tenantId, datacenterId, tenantDatacenterGrant);
        return ResponseEntity.ok(grant);
    }

    @Override
    @RequiresPermission(Permission.TENANT_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> updateTenantDatacenterGrant(
            UUID tenantId,
            UUID datacenterId,
            TenantDatacenterGrant tenantDatacenterGrant) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.updateTenantDatacenterGrant(tenantId, datacenterId, tenantDatacenterGrant);
        return ResponseEntity.ok(grant);
    }
}
