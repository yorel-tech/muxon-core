/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.controllers;

import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.db.model.PluginCatalogContributionEntity;
import com.yorel.muxon.services.plugin.CatalogApprovalService;
import com.yorel.muxon.services.plugin.CatalogContributionProvisioningService;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/catalog")
public class CatalogAdminController {

  @Autowired private CatalogContributionProvisioningService provisioningService;

  @Autowired(required = false)
  private CatalogApprovalService approvalService;

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
