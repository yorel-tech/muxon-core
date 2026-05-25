package com.sal.muxon.controllers;

import com.sal.muxon.api.OverviewApi;
import com.sal.muxon.api.model.SystemOverview;
import com.sal.muxon.api.model.TenantOverview;
import com.sal.muxon.auth.Permission;
import com.sal.muxon.auth.RequiresPermission;
import com.sal.muxon.services.OverviewService;
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
