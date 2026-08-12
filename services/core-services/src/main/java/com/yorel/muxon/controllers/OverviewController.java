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

import com.yorel.muxon.api.OverviewApi;
import com.yorel.muxon.api.model.SystemOverview;
import com.yorel.muxon.api.model.TenantOverview;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.OverviewService;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OverviewController implements OverviewApi {

  @Autowired private OverviewService overviewService;

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
