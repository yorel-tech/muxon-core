package com.krito.muxon.controllers;

import com.krito.muxon.api.OverviewApi;
import com.krito.muxon.api.model.SystemOverview;
import com.krito.muxon.api.model.TenantOverview;
import com.krito.muxon.auth.Permission;
import com.krito.muxon.auth.RequiresPermission;
import com.krito.muxon.services.OverviewService;
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
