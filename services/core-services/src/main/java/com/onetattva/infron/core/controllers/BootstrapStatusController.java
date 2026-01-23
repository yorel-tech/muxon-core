package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.dto.BootstrapStatusDto;
import com.onetattva.infron.core.services.SystemInitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public controller for checking bootstrap status.
 * Provides GET /status endpoint that returns only the bootstrap status enum.
 * No authentication required for this endpoint.
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
}
