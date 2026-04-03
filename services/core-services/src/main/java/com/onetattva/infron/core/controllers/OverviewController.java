package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.OverviewApi;
import com.onetattva.infron.api.model.SystemOverview;
import com.onetattva.infron.api.model.TenantOverview;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.OverviewService;
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
