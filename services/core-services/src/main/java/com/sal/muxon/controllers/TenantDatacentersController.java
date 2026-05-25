package com.sal.muxon.controllers;

import com.sal.muxon.api.TenantDatacentersApi;
import com.sal.muxon.api.model.GetTenantDatacenterEffective200Response;
import com.sal.muxon.api.model.TenantDatacenterGrant;
import com.sal.muxon.api.model.TenantDatacenterGrantCreate;
import com.sal.muxon.api.model.TenantDatacenterGrantList;
import com.sal.muxon.auth.Permission;
import com.sal.muxon.auth.RequiresAnyPermission;
import com.sal.muxon.auth.RequiresPermission;
import com.sal.muxon.services.TenantDatacenterGrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class TenantDatacentersController implements TenantDatacentersApi {

    @Autowired
    private TenantDatacenterGrantService tenantDatacenterGrantService;

    @Override
    @RequiresPermission(Permission.DATACENTER_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> createTenantDatacenterGrant(
            UUID tenantId,
            TenantDatacenterGrantCreate tenantDatacenterGrantCreate,
            Integer page,
            Integer perPage) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.createTenantDatacenterGrant(tenantId, tenantDatacenterGrantCreate);
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
            UUID tenantId,
            UUID datacenterId) {
        GetTenantDatacenterEffective200Response response = tenantDatacenterGrantService.getTenantDatacenterEffective(tenantId, datacenterId);
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
    public ResponseEntity<TenantDatacenterGrant> getTenantDatacenterGrant(UUID tenantId, UUID datacenterId) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.getTenantDatacenterGrant(tenantId, datacenterId);
        return ResponseEntity.ok(grant);
    }

    @Override
    @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
    public ResponseEntity<TenantDatacenterGrantList> listTenantDatacenters(
            UUID tenantId,
            Integer page,
            Integer perPage) {
        TenantDatacenterGrantList list = tenantDatacenterGrantService.listTenantDatacenters(tenantId, page, perPage);
        return ResponseEntity.ok(list);
    }

    @Override
    @RequiresPermission(Permission.DATACENTER_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> replaceTenantDatacenterGrant(
            UUID tenantId,
            UUID datacenterId,
            TenantDatacenterGrant tenantDatacenterGrant) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.replaceTenantDatacenterGrant(tenantId, datacenterId, tenantDatacenterGrant);
        return ResponseEntity.ok(grant);
    }

    @Override
    @RequiresPermission(Permission.DATACENTER_MANAGE)
    public ResponseEntity<TenantDatacenterGrant> updateTenantDatacenterGrant(
            UUID tenantId,
            UUID datacenterId,
            TenantDatacenterGrant tenantDatacenterGrant) {
        TenantDatacenterGrant grant = tenantDatacenterGrantService.updateTenantDatacenterGrant(tenantId, datacenterId, tenantDatacenterGrant);
        return ResponseEntity.ok(grant);
    }

    @Override
    @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
    public ResponseEntity<java.util.List<String>> getTenantStorageClasses(UUID tenantId, UUID datacenterId) {
        java.util.List<String> storageClasses = tenantDatacenterGrantService.getEffectiveStorageClasses(tenantId, datacenterId);
        return ResponseEntity.ok(storageClasses);
    }

    @Override
    @RequiresAnyPermission({Permission.DATACENTER_READ, Permission.TENANT_DATACENTER_READ})
    public ResponseEntity<java.util.Map<String, com.sal.muxon.api.model.StorageUsage>> getTenantStorageUsage(
            UUID tenantId, 
            UUID datacenterId) {
        java.util.Map<String, com.sal.muxon.api.model.StorageUsage> usage = 
            tenantDatacenterGrantService.getStorageUsageByClass(tenantId, datacenterId);
        return ResponseEntity.ok(usage);
    }
}
