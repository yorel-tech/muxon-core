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

import com.yorel.muxon.api.dto.BootstrapStatusDto;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.services.SystemInitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for bootstrap status operations. Provides GET /status endpoint that returns only the
 * bootstrap status enum. The GET endpoint is publicly accessible without authentication. The PUT
 * /status/ready endpoint requires SYSTEM_SETTINGS authority.
 */
@RestController
@RequestMapping("/api/v1")
public class BootstrapStatusController {

  private final SystemInitService systemInitService;

  public BootstrapStatusController(SystemInitService systemInitService) {
    this.systemInitService = systemInitService;
  }

  /**
   * Get current bootstrap status. Returns only the bootstrap status enum (NOTREADY, BOOTSTRAPPED,
   * or READY). This endpoint is publicly accessible without authentication.
   *
   * @return ResponseEntity with BootstrapStatusDto containing only systemStatus
   */
  @GetMapping("/status")
  public ResponseEntity<BootstrapStatusDto> getBootstrapStatus() {
    BootstrapStatusDto status = systemInitService.getBootstrapStatus();
    return ResponseEntity.ok(status);
  }

  /**
   * Mark bootstrap as READY. Updates the bootstrap status to READY, indicating all required setup
   * steps have been completed. This endpoint requires SYSTEM_SETTINGS authority.
   *
   * @return ResponseEntity with BootstrapStatusDto containing the updated status
   */
  @PutMapping("/status/ready")
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<BootstrapStatusDto> markBootstrapAsReady() {
    systemInitService.markBootstrapAsReady();
    BootstrapStatusDto status = systemInitService.getBootstrapStatus();
    return ResponseEntity.ok(status);
  }
}
