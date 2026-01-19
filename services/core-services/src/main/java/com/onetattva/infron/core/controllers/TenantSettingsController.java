package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.services.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tenant settings management.
 * Requires system:admin or tenant:admin role.
 */
@RestController
@RequestMapping("/api/v1")
public class TenantSettingsController {

    @Autowired
    private SystemSettingsService settingsService;

    // ==================== Tenant Settings Endpoints ====================
    
    @GetMapping("/tenants/{tenantId}/settings")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SystemSettings> getTenantSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getTenantSettings(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/settings")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SystemSettings> updateTenantSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SystemSettings settings) {
        return ResponseEntity.ok(settingsService.updateTenantSettings(tenantId, settings));
    }

    @GetMapping("/tenants/{tenantId}/settings/general")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> getTenantGeneralSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getGeneralSettings(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/settings/general")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> updateTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, settings));
    }

    @PatchMapping("/tenants/{tenantId}/settings/general")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> patchTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, settings));
    }

    @GetMapping("/tenants/{tenantId}/settings/security")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> getTenantSecuritySettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getSecuritySettings(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/settings/security")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> updateTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, settings));
    }

    @PatchMapping("/tenants/{tenantId}/settings/security")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> patchTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, settings));
    }

    @GetMapping("/tenants/{tenantId}/settings/notifications")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> getTenantNotificationSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getNotificationSettings(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/settings/notifications")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> updateTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, settings));
    }

    @PatchMapping("/tenants/{tenantId}/settings/notifications")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> patchTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, settings));
    }

    @GetMapping("/tenants/{tenantId}/settings/appearance")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> getTenantAppearanceSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getAppearanceSettings(tenantId));
    }

    @PutMapping("/tenants/{tenantId}/settings/appearance")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> updateTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, settings));
    }

    @PatchMapping("/tenants/{tenantId}/settings/appearance")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> patchTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, settings));
    }

    @PostMapping("/tenants/{tenantId}/settings/idp/validate")
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<IdpValidationResponse> validateIdp(
            @PathVariable UUID tenantId,
            @Valid @RequestBody IdpValidationRequest request) {
        return ResponseEntity.ok(settingsService.validateIdpConfiguration(request));
    }
}
