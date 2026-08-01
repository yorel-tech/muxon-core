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

import com.yorel.muxon.api.SystemSettingsApi;
import com.yorel.muxon.api.model.AppearanceSettings;
import com.yorel.muxon.api.model.GeneralSettings;
import com.yorel.muxon.api.model.IdpValidationRequest;
import com.yorel.muxon.api.model.IdpValidationResponse;
import com.yorel.muxon.api.model.SchemasIdpSettings;
import com.yorel.muxon.api.model.SchemasNotificationSettings;
import com.yorel.muxon.api.model.SecuritySettings;
import com.yorel.muxon.api.model.SystemSettings;
import com.yorel.muxon.api.model.TestEmailRequest;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.RequiresPermission;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.services.SystemSettingsService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for system and tenant settings management. Requires system:admin role for system
 * settings, tenant:admin role for tenant settings.
 */
@RestController
public class SystemSettingsController implements SystemSettingsApi {

  @Autowired private SystemSettingsService settingsService;

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
    return ResponseEntity.ok(
        settingsService.getGeneralSettings(UUID.fromString(Constants.SYSTEM_ID)));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<GeneralSettings> updateGeneralSettings(
      @Valid @RequestBody GeneralSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<GeneralSettings> patchGeneralSettings(
      @Valid @RequestBody GeneralSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateGeneralSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  // ==================== Security Settings Endpoints ====================

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SecuritySettings> getSecuritySettings() {
    return ResponseEntity.ok(
        settingsService.getSecuritySettings(UUID.fromString(Constants.SYSTEM_ID)));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SecuritySettings> updateSecuritySettings(
      @Valid @RequestBody SecuritySettings settings) {
    return ResponseEntity.ok(
        settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SecuritySettings> patchSecuritySettings(
      @Valid @RequestBody SecuritySettings settings) {
    return ResponseEntity.ok(
        settingsService.updateSecuritySettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  // ==================== Appearance Settings Endpoints ====================

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<AppearanceSettings> getAppearanceSettings() {
    return ResponseEntity.ok(
        settingsService.getAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID)));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<AppearanceSettings> updateAppearanceSettings(
      @Valid @RequestBody AppearanceSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<AppearanceSettings> patchAppearanceSettings(
      @Valid @RequestBody AppearanceSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateAppearanceSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  // ==================== Notification Settings Endpoints ====================

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SchemasNotificationSettings> getNotificationSettings() {
    return ResponseEntity.ok(
        settingsService.getNotificationSettings(UUID.fromString(Constants.SYSTEM_ID)));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SchemasNotificationSettings> updateNotificationSettings(
      @Valid @RequestBody SchemasNotificationSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SchemasNotificationSettings> patchNotificationSettings(
      @Valid @RequestBody SchemasNotificationSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateNotificationSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
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
    return ResponseEntity.ok(
        settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<SchemasIdpSettings> patchIdpSettings(
      @Valid @RequestBody SchemasIdpSettings settings) {
    return ResponseEntity.ok(
        settingsService.updateIdpSettings(UUID.fromString(Constants.SYSTEM_ID), settings));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<IdpValidationResponse> validateIdp(
      @Valid @RequestBody IdpValidationRequest request) {
    return ResponseEntity.ok(settingsService.validateIdpConfiguration(request));
  }

  @Override
  @RequiresPermission(Permission.SYSTEM_SETTINGS)
  public ResponseEntity<Void> disableIdp() {
    settingsService.disableIdp();
    return ResponseEntity.ok().build();
  }
}
