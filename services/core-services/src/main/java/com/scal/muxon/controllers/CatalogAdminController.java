package com.scal.muxon.controllers;

import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.RequiresPermission;
import com.scal.muxon.db.model.PluginCatalogContributionEntity;
import com.scal.muxon.services.plugin.CatalogApprovalService;
import com.scal.muxon.services.plugin.CatalogContributionProvisioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class CatalogAdminController {

    @Autowired private CatalogContributionProvisioningService provisioningService;
    @Autowired(required = false) private CatalogApprovalService approvalService;

    @GetMapping
    @RequiresPermission(Permission.PLUGIN_READ)
    public ResponseEntity<List<PluginCatalogContributionEntity>> listActiveCatalogItems() {
        return ResponseEntity.ok(provisioningService.listActiveCatalogContributions());
    }

    @PostMapping("/{itemId}/approve")
    @RequiresPermission(Permission.PLUGIN_MANAGE)
    public ResponseEntity<PluginCatalogContributionEntity> approveItem(@PathVariable UUID itemId) {
        if (approvalService == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(approvalService.approveItem(itemId));
    }
}
