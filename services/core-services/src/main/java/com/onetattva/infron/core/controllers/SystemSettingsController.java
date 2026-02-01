package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.SystemSettingsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.services.SystemSettingsService;
import com.onetattva.infron.db.model.IdentityProviderEntity;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for system and tenant settings management.
 * Requires system:admin role for system settings, tenant:admin role for tenant settings.
 */
@RestController
public class SystemSettingsController implements SystemSettingsApi {

    @Autowired
    private SystemSettingsService settingsService;

    // ==================== System Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SystemSettings> getSystemSettings() {
        return ResponseEntity.ok(settingsService.getSystemSettings());
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SystemSettings> updateSystemSettings(
            @Valid @RequestBody SystemSettings settings) {
        return ResponseEntity.ok(settingsService.updateSystemSettings(settings));
    }

    // ==================== General Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<GeneralSettings> getGeneralSettings() {
        return ResponseEntity.ok(settingsService.getGeneralSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<GeneralSettings> updateGeneralSettings(
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<GeneralSettings> patchGeneralSettings(
            @Valid @RequestBody GeneralSettings settings) {
        return ResponseEntity.ok(settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    // ==================== Security Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SecuritySettings> getSecuritySettings() {
        return ResponseEntity.ok(settingsService.getSecuritySettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SecuritySettings> updateSecuritySettings(
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SecuritySettings> patchSecuritySettings(
            @Valid @RequestBody SecuritySettings settings) {
        return ResponseEntity.ok(settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    // ==================== Appearance Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<AppearanceSettings> getAppearanceSettings() {
        return ResponseEntity.ok(settingsService.getAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<AppearanceSettings> updateAppearanceSettings(
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<AppearanceSettings> patchAppearanceSettings(
            @Valid @RequestBody AppearanceSettings settings) {
        return ResponseEntity.ok(settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    // ==================== Notification Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> getNotificationSettings() {
        return ResponseEntity.ok(settingsService.getNotificationSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> updateNotificationSettings(
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasNotificationSettings> patchNotificationSettings(
            @Valid @RequestBody SchemasNotificationSettings settings) {
        return ResponseEntity.ok(settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<Void> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        settingsService.sendTestEmail(UUID.fromString(Constants.SYSTEM_ID), request);
        return ResponseEntity.ok().build();
    }

    // ==================== IdP Settings Endpoints ====================

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasIdpSettings> getIdpSettings() {
        return ResponseEntity.ok(settingsService.getIdpSettings(UUID.fromString(Constants.SYSTEM_ID)));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasIdpSettings> updateIdpSettings(
            @Valid @RequestBody SchemasIdpSettings settings) {
        return ResponseEntity.ok(settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<SchemasIdpSettings> patchIdpSettings(
            @Valid @RequestBody SchemasIdpSettings settings) {
        return ResponseEntity.ok(settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<IdpValidationResponse> validateIdp(@Valid @RequestBody IdpValidationRequest request) {
        return ResponseEntity.ok(settingsService.validateIdpConfiguration(request));
    }

    @Override
    @RequiresPermission(Permission.SYSTEM_SETTINGS)
    public ResponseEntity<Void> disableIdp() {
        settingsService.disableIdp();
        return ResponseEntity.ok().build();
    }

}
