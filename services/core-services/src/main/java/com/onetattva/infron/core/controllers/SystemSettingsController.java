package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.SystemSettingsApi;
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
 * REST controller for system and tenant settings management.
 * Requires system:admin role for system settings, tenant:admin role for tenant settings.
 */
@RestController
@RequestMapping("/api/v1")
public class SystemSettingsController implements SystemSettingsApi {

    @Autowired
    private SystemSettingsService settingsService;

    // ==================== System Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SystemSettings> getSystemSettings() {
        return ResponseEntity.ok(settingsService.getSystemSettings());
    }

    @Override
    @PutMapping("/system-settings")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SystemSettings> updateSystemSettings(
            @Valid @RequestBody SystemSettings settings) {
        return ResponseEntity.ok(settingsService.updateSystemSettings(settings));
    }

    // ==================== General Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings/general")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<GeneralSettings> getGeneralSettings() {
        return ResponseEntity.ok(settingsService.getGeneralSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @PutMapping("/system-settings/general")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<GeneralSettings> updateGeneralSettings(
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PatchMapping("/system-settings/general")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<GeneralSettings> patchGeneralSettings(
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    // ==================== Security Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings/security")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SecuritySettings> getSecuritySettings() {
        return ResponseEntity.ok(settingsService.getSecuritySettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @PutMapping("/system-settings/security")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SecuritySettings> updateSecuritySettings(
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PatchMapping("/system-settings/security")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SecuritySettings> patchSecuritySettings(
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    // ==================== Notification Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings/notifications")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasNotificationSettings> getNotificationSettings() {
        return ResponseEntity.ok(settingsService.getNotificationSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @PutMapping("/system-settings/notifications")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasNotificationSettings> updateNotificationSettings(
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PatchMapping("/system-settings/notifications")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasNotificationSettings> patchNotificationSettings(
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PostMapping("/system-settings/notifications/test-email")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<Void> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        settingsService.sendTestEmail(UUID.fromString(Constants.SYSTEM_ID), request);
        return ResponseEntity.ok().build();
    }

    // ==================== IdP Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings/idp")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasIdpSettings> getIdpSettings() {
        return ResponseEntity.ok(settingsService.getIdpSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @PutMapping("/system-settings/idp")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasIdpSettings> updateIdpSettings(
            @Valid @RequestBody SchemasIdpSettings settings) {
        return ResponseEntity.ok(settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PatchMapping("/system-settings/idp")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<SchemasIdpSettings> patchIdpSettings(
            @Valid @RequestBody SchemasIdpSettings settings) {
        return ResponseEntity.ok(settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PostMapping("/system-settings/idp/validate")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<IdpValidationResponse> validateIdp(@Valid @RequestBody IdpValidationRequest request) {
        return ResponseEntity.ok(settingsService.validateIdpConfiguration(request));
    }

    @Override
    @DeleteMapping("/system-settings/idp")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<Void> disableIdp() {
        settingsService.disableIdp();
        return ResponseEntity.ok().build();
    }

    // ==================== Appearance Settings Endpoints ====================

    @Override
    @GetMapping("/system-settings/appearance")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<AppearanceSettings> getAppearanceSettings() {
        return ResponseEntity.ok(settingsService.getAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @PutMapping("/system-settings/appearance")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<AppearanceSettings> updateAppearanceSettings(
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @PatchMapping("/system-settings/appearance")
    @PreAuthorize("hasRole('system:admin')")
    public ResponseEntity<AppearanceSettings> patchAppearanceSettings(
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

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
}
