package com.sal.muxon.controllers;

import com.sal.muxon.api.TenantSettingsApi;
import com.sal.muxon.api.model.AppearanceSettings;
import com.sal.muxon.api.model.GeneralSettings;
import com.sal.muxon.api.model.SchemasIdpSettings;
import com.sal.muxon.api.model.SchemasNotificationSettings;
import com.sal.muxon.api.model.SecuritySettings;
import com.sal.muxon.auth.Permission;
import com.sal.muxon.auth.RequiresPermission;
import com.sal.muxon.services.SystemSettingsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for tenant settings management.
 * Tenant settings are read-only for IdP settings (system IdP is shared).
 */
@RestController
public class TenantSettingsController implements TenantSettingsApi {

    @Autowired
    private SystemSettingsService settingsService;

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<AppearanceSettings> getTenantAppearanceSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getAppearanceSettings(tenantId));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<GeneralSettings> getTenantGeneralSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getGeneralSettings(tenantId));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> getTenantNotificationSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getNotificationSettings(tenantId));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SecuritySettings> getTenantSecuritySettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getSecuritySettings(tenantId));
    }

    /**
     * Get IdP settings for a tenant.
     * Returns system IdP settings with masked secrets (read-only).
     * Tenants cannot modify IdP settings.
     */
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SchemasIdpSettings> getTenantIdpSettings(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(settingsService.getIdpSettings(tenantId));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<AppearanceSettings> patchTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings appearanceSettings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, appearanceSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<GeneralSettings> patchTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings generalSettings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, generalSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> patchTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings schemasNotificationSettings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, schemasNotificationSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SecuritySettings> patchTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings securitySettings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, securitySettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<AppearanceSettings> updateTenantAppearanceSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody AppearanceSettings appearanceSettings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(tenantId, appearanceSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<GeneralSettings> updateTenantGeneralSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody GeneralSettings generalSettings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(tenantId, generalSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> updateTenantNotificationSettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SchemasNotificationSettings schemasNotificationSettings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(tenantId, schemasNotificationSettings));
    }

    @Override
    @RequiresPermission(Permission.TENANT_SETTINGS)
    public ResponseEntity<SecuritySettings> updateTenantSecuritySettings(
            @PathVariable UUID tenantId,
            @Valid @RequestBody SecuritySettings securitySettings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(tenantId, securitySettings));
    }
}
