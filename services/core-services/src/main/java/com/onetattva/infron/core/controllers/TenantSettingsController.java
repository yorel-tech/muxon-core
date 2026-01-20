package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.TenantSettingsApi;
import com.onetattva.infron.api.model.AppearanceSettings;
import com.onetattva.infron.api.model.GeneralSettings;
import com.onetattva.infron.api.model.SchemasNotificationSettings;
import com.onetattva.infron.api.model.SecuritySettings;
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
public class TenantSettingsController implements TenantSettingsApi {

    @Autowired
    private SystemSettingsService settingsService;

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> getTenantAppearanceSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getAppearanceSettings(tenantId));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> getTenantGeneralSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getGeneralSettings(tenantId));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> getTenantNotificationSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getNotificationSettings(tenantId));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> getTenantSecuritySettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getSecuritySettings(tenantId));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> patchTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings appearanceSettings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, appearanceSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> patchTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings generalSettings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, generalSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> patchTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings schemasNotificationSettings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, schemasNotificationSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> patchTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings securitySettings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, securitySettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<AppearanceSettings> updateTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings appearanceSettings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, appearanceSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<GeneralSettings> updateTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings generalSettings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, generalSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SchemasNotificationSettings> updateTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings schemasNotificationSettings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, schemasNotificationSettings));
    }

    @Override
    @PreAuthorize("hasRole('system:admin') or hasRole('tenant:admin')")
    public ResponseEntity<SecuritySettings> updateTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings securitySettings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, securitySettings));
    }
}
