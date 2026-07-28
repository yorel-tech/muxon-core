package com.yorel.muxon.controllers;

import com.yorel.muxon.api.OverviewApi;
import com.yorel.muxon.api.model.SystemOverview;
import com.yorel.muxon.api.model.TenantOverview;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.OverviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class OverviewController implements OverviewApi {

    @Autowired
    private OverviewService overviewService;

    @Override
    @RequiresPermission(Permission.TENANT_READ)
    public ResponseEntity<SystemOverview> getSystemOverview() {
        return ResponseEntity.ok(overviewService.getSystemOverview());
    }

    @Override
    @RequiresPermission(Permission.TENANT_READ)
    public ResponseEntity<TenantOverview> getTenantOverview(UUID tenantId) {
        return ResponseEntity.ok(overviewService.getTenantOverview(tenantId));
    }
}
