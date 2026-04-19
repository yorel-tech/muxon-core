package com.krito.muxon.controllers;

import com.krito.muxon.api.dto.BootstrapStatusDto;
import com.krito.muxon.auth.Permission;
import com.krito.muxon.auth.RequiresPermission;
import com.krito.muxon.services.SystemInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for bootstrap status operations.
 * Provides GET /status endpoint that returns only the bootstrap status enum.
 * The GET endpoint is publicly accessible without authentication.
 * The PUT /status/ready endpoint requires SYSTEM_SETTINGS authority.
 */
@RestController
@RequestMapping("/api/v1")
public class BootstrapStatusController {

    private final SystemInitService systemInitService;

    public BootstrapStatusController(SystemInitService systemInitService) {
        this.systemInitService = systemInitService;
    }

    /**
     * Get current bootstrap status.
     * Returns only the bootstrap status enum (NOTREADY, BOOTSTRAPPED, or READY).
     * This endpoint is publicly accessible without authentication.
     *
     * @return ResponseEntity with BootstrapStatusDto containing only systemStatus
     */
    @GetMapping("/status")
    public ResponseEntity<BootstrapStatusDto> getBootstrapStatus() {
        BootstrapStatusDto status = systemInitService.getBootstrapStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Mark bootstrap as READY.
     * Updates the bootstrap status to READY, indicating all required setup steps have been completed.
     * This endpoint requires SYSTEM_SETTINGS authority.
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
