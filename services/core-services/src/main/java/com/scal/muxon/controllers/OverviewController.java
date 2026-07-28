package com.scal.muxon.controllers;

import com.scal.muxon.api.OverviewApi;
import com.scal.muxon.api.model.SystemOverview;
import com.scal.muxon.api.model.TenantOverview;
import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.RequiresPermission;
import com.scal.muxon.services.OverviewService;
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
