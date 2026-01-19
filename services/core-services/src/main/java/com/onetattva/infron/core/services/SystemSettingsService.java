package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.common.EncryptionUtil;
import com.onetattva.infron.db.model.SystemSettingsEntity;
import com.onetattva.infron.db.repository.SystemSettingsRepository;
import com.onetattva.infron.db.repository.IdentityProviderRepository;
import com.onetattva.infron.db.model.IdentityProviderEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service for managing system and tenant settings.
 * Implements tenant override pattern where tenant-specific settings
 * override system defaults.
 */
@Service
public class SystemSettingsService {

    @Autowired
    private SystemSettingsRepository settingsRepository;

    @Autowired
    private IdentityProviderRepository idpRepository;

    /**
     * Get system settings (tenant_id = SYSTEM_ID)
     */
    public SystemSettings getSystemSettings() {
        Optional<SystemSettingsEntity> systemSettings = 
            settingsRepository.findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
        
        if (systemSettings.isEmpty()) {
            throw new EntityNotFoundException("System settings not found. Please initialize during bootstrap.");
        }
        
        return mapEntityToDto(systemSettings.get(), true);
    }

    /**
     * Update system settings
     */
    @Transactional
    public SystemSettings updateSystemSettings(SystemSettings settings) {
        Optional<SystemSettingsEntity> existing = 
            settingsRepository.findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
        
        SystemSettingsEntity entity = existing.orElseGet(() -> new SystemSettingsEntity());
        updateEntityFromDto(entity, settings);
        entity.setTenantId(UUID.fromString(Constants.SYSTEM_ID));
        entity.setUpdatedAt(Instant.now());
        
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapEntityToDto(saved, true);
    }

    /**
     * Get tenant settings (merged with system defaults)
     */
    public SystemSettings getTenantSettings(UUID tenantId) {
        // Get system defaults
        Optional<SystemSettingsEntity> systemSettingsOpt = 
            settingsRepository.findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
        SystemSettingsEntity systemDefaults = systemSettingsOpt.orElse(null);
        
        // Get tenant overrides
        Optional<SystemSettingsEntity> tenantSettingsOpt = 
            settingsRepository.findByTenantId(tenantId);
        
        if (tenantSettingsOpt.isPresent()) {
            // Merge tenant overrides with system defaults
            return mergeSettings(systemDefaults, tenantSettingsOpt.get());
        } else {
            // No tenant override, use system defaults
            return mapEntityToDto(systemDefaults, true);
        }
    }

    /**
     * Update tenant settings
     */
    @Transactional
    public SystemSettings updateTenantSettings(UUID tenantId, SystemSettings settings) {
        Optional<SystemSettingsEntity> existing = 
            settingsRepository.findByTenantId(tenantId);
        
        SystemSettingsEntity entity = existing.orElseThrow(() -> 
            new EntityNotFoundException("Tenant settings not found for tenant: " + tenantId));
        updateEntityFromDto(entity, settings);
        entity.setTenantId(tenantId);
        entity.setUpdatedAt(Instant.now());
        
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapEntityToDto(saved, false);
    }

    /**
     * Get general settings for a tenant
     */
    public GeneralSettings getGeneralSettings(UUID tenantId) {
        Optional<SystemSettingsEntity> settingsOpt = settingsRepository.findByTenantId(tenantId);
        if (settingsOpt.isEmpty()) {
            throw new EntityNotFoundException("Settings not found for tenant: " + tenantId);
        }
        return mapGeneralSettings(settingsOpt.get());
    }

    /**
     * Update general settings for a tenant
     */
    @Transactional
    public GeneralSettings updateGeneralSettings(UUID tenantId, GeneralSettings settings) {
        Optional<SystemSettingsEntity> existing = settingsRepository.findByTenantId(tenantId);
        SystemSettingsEntity entity = existing.orElseGet(() -> {
            SystemSettingsEntity newEntity = new SystemSettingsEntity();
            newEntity.setTenantId(tenantId);
            newEntity.setCreatedAt(Instant.now());
            return newEntity;
        });
        
        if (settings.getName() != null) entity.setName(settings.getName());
        if (settings.getDescription() != null) entity.setDescription(settings.getDescription());
        if (settings.getContactEmail() != null) entity.setContactEmail(settings.getContactEmail());
        if (settings.getContactPhone() != null) entity.setContactPhone(settings.getContactPhone());
        if (settings.getDefaultTimezone() != null) entity.setDefaultTimezone(settings.getDefaultTimezone());
        if (settings.getDefaultLocale() != null) entity.setDefaultLocale(settings.getDefaultLocale());
        
        entity.setUpdatedAt(Instant.now());
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapGeneralSettings(saved);
    }

    /**
     * Get security settings for a tenant
     */
    public SecuritySettings getSecuritySettings(UUID tenantId) {
        Optional<SystemSettingsEntity> settingsOpt = settingsRepository.findByTenantId(tenantId);
        if (settingsOpt.isEmpty()) {
            throw new EntityNotFoundException("Settings not found for tenant: " + tenantId);
        }
        return mapSecuritySettings(settingsOpt.get());
    }

    /**
     * Update security settings for a tenant
     */
    @Transactional
    public SecuritySettings updateSecuritySettings(UUID tenantId, SecuritySettings settings) {
        Optional<SystemSettingsEntity> existing = settingsRepository.findByTenantId(tenantId);
        SystemSettingsEntity entity = existing.orElseGet(() -> {
            SystemSettingsEntity newEntity = new SystemSettingsEntity();
            newEntity.setTenantId(tenantId);
            newEntity.setCreatedAt(Instant.now());
            return newEntity;
        });
        
        if (settings.getSessionTimeoutMinutes() != null) entity.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        if (settings.getMaxLoginAttempts() != null) entity.setMaxLoginAttempts(settings.getMaxLoginAttempts());
        if (settings.getLockoutDurationMinutes() != null) entity.setLockoutDurationMinutes(settings.getLockoutDurationMinutes());
        if (settings.getPasswordMinLength() != null) entity.setPasswordMinLength(settings.getPasswordMinLength());
        if (settings.getPasswordRequireUppercase() != null) entity.setPasswordRequireUppercase(settings.getPasswordRequireUppercase());
        if (settings.getPasswordRequireLowercase() != null) entity.setPasswordRequireLowercase(settings.getPasswordRequireLowercase());
        if (settings.getPasswordRequireDigit() != null) entity.setPasswordRequireDigit(settings.getPasswordRequireDigit());
        if (settings.getPasswordRequireSymbol() != null) entity.setPasswordRequireSymbol(settings.getPasswordRequireSymbol());
        if (settings.getPasswordExpiryDays() != null) entity.setPasswordExpiryDays(settings.getPasswordExpiryDays());
        if (settings.getApiRateLimitPerMinute() != null) entity.setApiRateLimitPerMinute(settings.getApiRateLimitPerMinute());
        
        entity.setUpdatedAt(Instant.now());
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapSecuritySettings(saved);
    }

    /**
     * Get notification settings for a tenant
     */
    public SchemasNotificationSettings getNotificationSettings(UUID tenantId) {
        Optional<SystemSettingsEntity> settingsOpt = settingsRepository.findByTenantId(tenantId);
        if (settingsOpt.isEmpty()) {
            throw new EntityNotFoundException("Settings not found for tenant: " + tenantId);
        }
        return mapNotificationSettings(settingsOpt.get());
    }

    /**
     * Update notification settings for a tenant
     */
    @Transactional
    public SchemasNotificationSettings updateNotificationSettings(UUID tenantId, SchemasNotificationSettings settings) {
        Optional<SystemSettingsEntity> existing = settingsRepository.findByTenantId(tenantId);
        SystemSettingsEntity entity = existing.orElseGet(() -> {
            SystemSettingsEntity newEntity = new SystemSettingsEntity();
            newEntity.setTenantId(tenantId);
            newEntity.setCreatedAt(Instant.now());
            return newEntity;
        });
        
        if (settings.getSmtpPassword() != null && !"*****".equals(settings.getSmtpPassword())) {
            String encrypted = EncryptionUtil.encrypt(settings.getSmtpPassword());
            entity.setSmtpPassword(encrypted);
        }
        if (settings.getSmtpEnabled() != null) entity.setSmtpEnabled(settings.getSmtpEnabled());
        if (settings.getSmtpHost() != null) entity.setSmtpHost(settings.getSmtpHost());
        if (settings.getSmtpPort() != null) entity.setSmtpPort(settings.getSmtpPort());
        if (settings.getSmtpUsername() != null) entity.setSmtpUsername(settings.getSmtpUsername());
        if (settings.getSmtpFromEmail() != null) entity.setSmtpFromEmail(settings.getSmtpFromEmail());
        if (settings.getSmtpUseTls() != null) entity.setSmtpUseTls(settings.getSmtpUseTls());
        if (settings.getSlackWebhookUrl() != null) entity.setSlackWebhookUrl(settings.getSlackWebhookUrl().toString());
        if (settings.getWebhookEnabled() != null) entity.setWebhookEnabled(settings.getWebhookEnabled());
        
        entity.setUpdatedAt(Instant.now());
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapNotificationSettings(saved);
    }



    /**
     * Get IdP settings for a tenant
     */
    public SchemasIdpSettings getIdpSettings(UUID tenantId) {
        SchemasIdpSettings idpSettings = new SchemasIdpSettings();
        idpSettings.setEnabled(false);
        return idpSettings;
    }

    /**
     * Update IdP settings for a tenant
     */
    @Transactional
    public SchemasIdpSettings updateIdpSettings(UUID tenantId, SchemasIdpSettings settings) {
        SchemasIdpSettings result = new SchemasIdpSettings();
        result.setEnabled(settings.getEnabled() != null ? settings.getEnabled() : false);
        result.setType(settings.getType());
        result.setName(settings.getName());
        result.setIssuerUrl(settings.getIssuerUrl());
        result.setClientId(settings.getClientId());
        result.setClientSecret(settings.getClientSecret());
        result.setScopes(settings.getScopes());
        result.setAutoProvisionUsers(settings.getAutoProvisionUsers());
        result.setJwksUri(settings.getJwksUri());
        result.setUserinfoEndpoint(settings.getUserinfoEndpoint());
        return result;
    }

    /**
     * Disable IdP for a tenant
     */
    @Transactional
    public void disableIdp() {
        // Implementation would delete IdP configuration
        // For now, just a placeholder
    }

    /**
     * Get appearance settings for a tenant
     */
    public AppearanceSettings getAppearanceSettings(UUID tenantId) {
        Optional<SystemSettingsEntity> settingsOpt = settingsRepository.findByTenantId(tenantId);
        if (settingsOpt.isEmpty()) {
            throw new EntityNotFoundException("Settings not found for tenant: " + tenantId);
        }
        return mapAppearanceSettings(settingsOpt.get());
    }

    /**
     * Update appearance settings for a tenant
     */
    @Transactional
    public AppearanceSettings updateAppearanceSettings(UUID tenantId, AppearanceSettings settings) {
        Optional<SystemSettingsEntity> existing = settingsRepository.findByTenantId(tenantId);
        SystemSettingsEntity entity = existing.orElseGet(() -> {
            SystemSettingsEntity newEntity = new SystemSettingsEntity();
            newEntity.setTenantId(tenantId);
            newEntity.setCreatedAt(Instant.now());
            return newEntity;
        });
        
        if (settings.getTheme() != null) entity.setTheme(settings.getTheme().name());
        if (settings.getLogoUrl() != null) entity.setLogoUrl(settings.getLogoUrl().toString());
        if (settings.getFaviconUrl() != null) entity.setFaviconUrl(settings.getFaviconUrl().toString());
        if (settings.getPrimaryColor() != null) entity.setPrimaryColor(settings.getPrimaryColor());
        if (settings.getSecondaryColor() != null) entity.setSecondaryColor(settings.getSecondaryColor());
        if (settings.getCustomCssUrl() != null) entity.setCustomCssUrl(settings.getCustomCssUrl().toString());
        
        entity.setUpdatedAt(Instant.now());
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapAppearanceSettings(saved);
    }

    /**
     * Merge tenant settings with system defaults
     * Tenant values take precedence over system defaults
     */
    private SystemSettings mergeSettings(SystemSettingsEntity systemDefaults, SystemSettingsEntity tenantSettings) {
        SystemSettingsEntity merged = new SystemSettingsEntity();
        merged.setId(tenantSettings.getId());
        merged.setTenantId(tenantSettings.getTenantId());
        merged.setCreatedAt(systemDefaults.getCreatedAt());
        merged.setUpdatedAt(Instant.now());
        
        // General Settings - tenant override
        merged.setName(coalesce(tenantSettings.getName(), systemDefaults.getName()));
        merged.setDescription(coalesce(tenantSettings.getDescription(), systemDefaults.getDescription()));
        merged.setContactEmail(coalesce(tenantSettings.getContactEmail(), systemDefaults.getContactEmail()));
        merged.setContactPhone(coalesce(tenantSettings.getContactPhone(), systemDefaults.getContactPhone()));
        merged.setDefaultTimezone(coalesce(tenantSettings.getDefaultTimezone(), systemDefaults.getDefaultTimezone()));
        merged.setDefaultLocale(coalesce(tenantSettings.getDefaultLocale(), systemDefaults.getDefaultLocale()));
        
        // Security Settings - tenant override
        merged.setSessionTimeoutMinutes(coalesce(tenantSettings.getSessionTimeoutMinutes(), systemDefaults.getSessionTimeoutMinutes()));
        merged.setMaxLoginAttempts(coalesce(tenantSettings.getMaxLoginAttempts(), systemDefaults.getMaxLoginAttempts()));
        merged.setLockoutDurationMinutes(coalesce(tenantSettings.getLockoutDurationMinutes(), systemDefaults.getLockoutDurationMinutes()));
        merged.setPasswordMinLength(coalesce(tenantSettings.getPasswordMinLength(), systemDefaults.getPasswordMinLength()));
        merged.setPasswordRequireUppercase(coalesce(tenantSettings.getPasswordRequireUppercase(), systemDefaults.getPasswordRequireUppercase()));
        merged.setPasswordRequireLowercase(coalesce(tenantSettings.getPasswordRequireLowercase(), systemDefaults.getPasswordRequireLowercase()));
        merged.setPasswordRequireDigit(coalesce(tenantSettings.getPasswordRequireDigit(), systemDefaults.getPasswordRequireDigit()));
        merged.setPasswordRequireSymbol(coalesce(tenantSettings.getPasswordRequireSymbol(), systemDefaults.getPasswordRequireSymbol()));
        merged.setPasswordExpiryDays(coalesce(tenantSettings.getPasswordExpiryDays(), systemDefaults.getPasswordExpiryDays()));
        merged.setApiRateLimitPerMinute(coalesce(tenantSettings.getApiRateLimitPerMinute(), systemDefaults.getApiRateLimitPerMinute()));
        
        // Notification Settings - tenant override
        merged.setSmtpEnabled(coalesce(tenantSettings.getSmtpEnabled(), systemDefaults.getSmtpEnabled()));
        merged.setSmtpHost(coalesce(tenantSettings.getSmtpHost(), systemDefaults.getSmtpHost()));
        merged.setSmtpPort(coalesce(tenantSettings.getSmtpPort(), systemDefaults.getSmtpPort()));
        merged.setSmtpUsername(coalesce(tenantSettings.getSmtpUsername(), systemDefaults.getSmtpUsername()));
        merged.setSmtpPassword(coalesce(tenantSettings.getSmtpPassword(), systemDefaults.getSmtpPassword()));
        merged.setSmtpFromEmail(coalesce(tenantSettings.getSmtpFromEmail(), systemDefaults.getSmtpFromEmail()));
        merged.setSmtpUseTls(coalesce(tenantSettings.getSmtpUseTls(), systemDefaults.getSmtpUseTls()));
        merged.setSlackWebhookUrl(coalesce(tenantSettings.getSlackWebhookUrl(), systemDefaults.getSlackWebhookUrl()));
        merged.setWebhookEnabled(coalesce(tenantSettings.getWebhookEnabled(), systemDefaults.getWebhookEnabled()));
        
        // Appearance Settings - tenant override
        merged.setTheme(coalesce(tenantSettings.getTheme(), systemDefaults.getTheme()));
        merged.setLogoUrl(coalesce(tenantSettings.getLogoUrl(), systemDefaults.getLogoUrl()));
        merged.setFaviconUrl(coalesce(tenantSettings.getFaviconUrl(), systemDefaults.getFaviconUrl()));
        merged.setPrimaryColor(coalesce(tenantSettings.getPrimaryColor(), systemDefaults.getPrimaryColor()));
        merged.setSecondaryColor(coalesce(tenantSettings.getSecondaryColor(), systemDefaults.getSecondaryColor()));
        merged.setCustomCssUrl(coalesce(tenantSettings.getCustomCssUrl(), systemDefaults.getCustomCssUrl()));
        
        return mapEntityToDto(merged, true);
    }

    /**
     * Map entity to DTO
     * @param maskSensitive - if true, mask sensitive fields (passwords)
     */
    private SystemSettings mapEntityToDto(SystemSettingsEntity entity, boolean maskSensitive) {
        SystemSettings dto = new SystemSettings();
        
        // General Settings
        GeneralSettings general = new GeneralSettings();
        general.setName(entity.getName());
        general.setDescription(entity.getDescription());
        general.setContactEmail(entity.getContactEmail());
        general.setContactPhone(entity.getContactPhone());
        general.setDefaultTimezone(entity.getDefaultTimezone());
        general.setDefaultLocale(entity.getDefaultLocale());
        dto.setGeneral(general);
        
        // Security Settings
        SecuritySettings security = new SecuritySettings();
        security.setSessionTimeoutMinutes(entity.getSessionTimeoutMinutes());
        security.setMaxLoginAttempts(entity.getMaxLoginAttempts());
        security.setLockoutDurationMinutes(entity.getLockoutDurationMinutes());
        security.setPasswordMinLength(entity.getPasswordMinLength());
        security.setPasswordRequireUppercase(entity.getPasswordRequireUppercase());
        security.setPasswordRequireLowercase(entity.getPasswordRequireLowercase());
        security.setPasswordRequireDigit(entity.getPasswordRequireDigit());
        security.setPasswordRequireSymbol(entity.getPasswordRequireSymbol());
        security.setPasswordExpiryDays(entity.getPasswordExpiryDays());
        security.setApiRateLimitPerMinute(entity.getApiRateLimitPerMinute());
        dto.setSecurity(security);
        
        // Notification Settings - mask password if requested
        SchemasNotificationSettings notifications = new SchemasNotificationSettings();
        if (maskSensitive && entity.getSmtpPassword() != null) {
            notifications.setSmtpPassword("*****"); // Always mask
        } else {
            notifications.setSmtpPassword(entity.getSmtpPassword());
        }
        notifications.setSmtpEnabled(entity.getSmtpEnabled());
        notifications.setSmtpHost(entity.getSmtpHost());
        notifications.setSmtpPort(entity.getSmtpPort());
        notifications.setSmtpUsername(entity.getSmtpUsername());
        notifications.setSmtpFromEmail(entity.getSmtpFromEmail());
        notifications.setSmtpUseTls(entity.getSmtpUseTls());
        if (entity.getSlackWebhookUrl() != null) {
            notifications.setSlackWebhookUrl(URI.create(entity.getSlackWebhookUrl()));
        }
        notifications.setWebhookEnabled(entity.getWebhookEnabled());
        dto.setNotifications(notifications);
        
        // IdP Settings
        SchemasIdpSettings idp = new SchemasIdpSettings();
        dto.setIdp(idp);
        
        // Appearance Settings
        AppearanceSettings appearance = new AppearanceSettings();
        if (entity.getTheme() != null) {
            appearance.setTheme(AppearanceSettings.ThemeEnum.fromValue(entity.getTheme()));
        }
        if (entity.getLogoUrl() != null) {
            appearance.setLogoUrl(URI.create(entity.getLogoUrl()));
        }
        if (entity.getFaviconUrl() != null) {
            appearance.setFaviconUrl(URI.create(entity.getFaviconUrl()));
        }
        appearance.setPrimaryColor(entity.getPrimaryColor());
        appearance.setSecondaryColor(entity.getSecondaryColor());
        if (entity.getCustomCssUrl() != null) {
            appearance.setCustomCssUrl(URI.create(entity.getCustomCssUrl()));
        }
        dto.setAppearance(appearance);
        
        // Audit fields
        dto.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        dto.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        dto.setUpdatedBy(entity.getUpdatedBy());
        
        return dto;
    }

    /**
     * Update entity from DTO
     * Encrypt sensitive fields before saving
     */
    private void updateEntityFromDto(SystemSettingsEntity entity, SystemSettings dto) {
        // General Settings
        if (dto.getGeneral() != null) {
            if (dto.getGeneral().getName() != null) entity.setName(dto.getGeneral().getName());
            if (dto.getGeneral().getDescription() != null) entity.setDescription(dto.getGeneral().getDescription());
            if (dto.getGeneral().getContactEmail() != null) entity.setContactEmail(dto.getGeneral().getContactEmail());
            if (dto.getGeneral().getContactPhone() != null) entity.setContactPhone(dto.getGeneral().getContactPhone());
            if (dto.getGeneral().getDefaultTimezone() != null) entity.setDefaultTimezone(dto.getGeneral().getDefaultTimezone());
            if (dto.getGeneral().getDefaultLocale() != null) entity.setDefaultLocale(dto.getGeneral().getDefaultLocale());
        }
        
        // Security Settings
        if (dto.getSecurity() != null) {
            if (dto.getSecurity().getSessionTimeoutMinutes() != null) entity.setSessionTimeoutMinutes(dto.getSecurity().getSessionTimeoutMinutes());
            if (dto.getSecurity().getMaxLoginAttempts() != null) entity.setMaxLoginAttempts(dto.getSecurity().getMaxLoginAttempts());
            if (dto.getSecurity().getLockoutDurationMinutes() != null) entity.setLockoutDurationMinutes(dto.getSecurity().getLockoutDurationMinutes());
            if (dto.getSecurity().getPasswordMinLength() != null) entity.setPasswordMinLength(dto.getSecurity().getPasswordMinLength());
            if (dto.getSecurity().getPasswordRequireUppercase() != null) entity.setPasswordRequireUppercase(dto.getSecurity().getPasswordRequireUppercase());
            if (dto.getSecurity().getPasswordRequireLowercase() != null) entity.setPasswordRequireLowercase(dto.getSecurity().getPasswordRequireLowercase());
            if (dto.getSecurity().getPasswordRequireDigit() != null) entity.setPasswordRequireDigit(dto.getSecurity().getPasswordRequireDigit());
            if (dto.getSecurity().getPasswordRequireSymbol() != null) entity.setPasswordRequireSymbol(dto.getSecurity().getPasswordRequireSymbol());
            if (dto.getSecurity().getPasswordExpiryDays() != null) entity.setPasswordExpiryDays(dto.getSecurity().getPasswordExpiryDays());
            if (dto.getSecurity().getApiRateLimitPerMinute() != null) entity.setApiRateLimitPerMinute(dto.getSecurity().getApiRateLimitPerMinute());
        }
        
        // Notification Settings - encrypt password if provided and not masked
        if (dto.getNotifications() != null) {
            if (dto.getNotifications().getSmtpPassword() != null && !"*****".equals(dto.getNotifications().getSmtpPassword())) {
                String encrypted = EncryptionUtil.encrypt(dto.getNotifications().getSmtpPassword());
                entity.setSmtpPassword(encrypted);
            }
            if (dto.getNotifications().getSmtpEnabled() != null) entity.setSmtpEnabled(dto.getNotifications().getSmtpEnabled());
            if (dto.getNotifications().getSmtpHost() != null) entity.setSmtpHost(dto.getNotifications().getSmtpHost());
            if (dto.getNotifications().getSmtpPort() != null) entity.setSmtpPort(dto.getNotifications().getSmtpPort());
            if (dto.getNotifications().getSmtpUsername() != null) entity.setSmtpUsername(dto.getNotifications().getSmtpUsername());
            if (dto.getNotifications().getSmtpFromEmail() != null) entity.setSmtpFromEmail(dto.getNotifications().getSmtpFromEmail());
            if (dto.getNotifications().getSmtpUseTls() != null) entity.setSmtpUseTls(dto.getNotifications().getSmtpUseTls());
            if (dto.getNotifications().getSlackWebhookUrl() != null) entity.setSlackWebhookUrl(dto.getNotifications().getSlackWebhookUrl().toString());
            if (dto.getNotifications().getWebhookEnabled() != null) entity.setWebhookEnabled(dto.getNotifications().getWebhookEnabled());
        }
        
        // Appearance Settings
        if (dto.getAppearance() != null) {
            if (dto.getAppearance().getTheme() != null) entity.setTheme(dto.getAppearance().getTheme().name());
            if (dto.getAppearance().getLogoUrl() != null) entity.setLogoUrl(dto.getAppearance().getLogoUrl().toString());
            if (dto.getAppearance().getFaviconUrl() != null) entity.setFaviconUrl(dto.getAppearance().getFaviconUrl().toString());
            if (dto.getAppearance().getPrimaryColor() != null) entity.setPrimaryColor(dto.getAppearance().getPrimaryColor());
            if (dto.getAppearance().getSecondaryColor() != null) entity.setSecondaryColor(dto.getAppearance().getSecondaryColor());
            if (dto.getAppearance().getCustomCssUrl() != null) entity.setCustomCssUrl(dto.getAppearance().getCustomCssUrl().toString());
        }
    }

    /**
     * Map general settings from entity
     */
    private GeneralSettings mapGeneralSettings(SystemSettingsEntity entity) {
        GeneralSettings settings = new GeneralSettings();
        settings.setName(entity.getName());
        settings.setDescription(entity.getDescription());
        settings.setContactEmail(entity.getContactEmail());
        settings.setContactPhone(entity.getContactPhone());
        settings.setDefaultTimezone(entity.getDefaultTimezone());
        settings.setDefaultLocale(entity.getDefaultLocale());
        return settings;
    }

    /**
     * Map security settings from entity
     */
    private SecuritySettings mapSecuritySettings(SystemSettingsEntity entity) {
        SecuritySettings settings = new SecuritySettings();
        settings.setSessionTimeoutMinutes(entity.getSessionTimeoutMinutes());
        settings.setMaxLoginAttempts(entity.getMaxLoginAttempts());
        settings.setLockoutDurationMinutes(entity.getLockoutDurationMinutes());
        settings.setPasswordMinLength(entity.getPasswordMinLength());
        settings.setPasswordRequireUppercase(entity.getPasswordRequireUppercase());
        settings.setPasswordRequireLowercase(entity.getPasswordRequireLowercase());
        settings.setPasswordRequireDigit(entity.getPasswordRequireDigit());
        settings.setPasswordRequireSymbol(entity.getPasswordRequireSymbol());
        settings.setPasswordExpiryDays(entity.getPasswordExpiryDays());
        settings.setApiRateLimitPerMinute(entity.getApiRateLimitPerMinute());
        return settings;
    }

    /**
     * Map notification settings from entity
     */
    private SchemasNotificationSettings mapNotificationSettings(SystemSettingsEntity entity) {
        SchemasNotificationSettings settings = new SchemasNotificationSettings();
        settings.setSmtpEnabled(entity.getSmtpEnabled());
        settings.setSmtpHost(entity.getSmtpHost());
        settings.setSmtpPort(entity.getSmtpPort());
        settings.setSmtpUsername(entity.getSmtpUsername());
        settings.setSmtpPassword("*****"); // Always mask
        settings.setSmtpFromEmail(entity.getSmtpFromEmail());
        settings.setSmtpUseTls(entity.getSmtpUseTls());
        if (entity.getSlackWebhookUrl() != null) {
            settings.setSlackWebhookUrl(URI.create(entity.getSlackWebhookUrl()));
        }
        settings.setWebhookEnabled(entity.getWebhookEnabled());
        return settings;
    }

    /**
     * Map appearance settings from entity
     */
    private AppearanceSettings mapAppearanceSettings(SystemSettingsEntity entity) {
        AppearanceSettings settings = new AppearanceSettings();
        if (entity.getTheme() != null) {
            settings.setTheme(AppearanceSettings.ThemeEnum.fromValue(entity.getTheme()));
        }
        if (entity.getLogoUrl() != null) {
            settings.setLogoUrl(URI.create(entity.getLogoUrl()));
        }
        if (entity.getFaviconUrl() != null) {
            settings.setFaviconUrl(URI.create(entity.getFaviconUrl()));
        }
        settings.setPrimaryColor(entity.getPrimaryColor());
        settings.setSecondaryColor(entity.getSecondaryColor());
        if (entity.getCustomCssUrl() != null) {
            settings.setCustomCssUrl(URI.create(entity.getCustomCssUrl()));
        }
        return settings;
    }

    /**
     * Get system-level identity provider
     */
    public Optional<IdentityProviderEntity> getSystemIdentityProvider() {
        List<IdentityProviderEntity> systemProviders = idpRepository.findSystemProvider();
        return systemProviders.isEmpty() ? Optional.empty() : Optional.of(systemProviders.get(0));
    }

    /**
     * Helper method for coalescing values
     */
    private <T> T coalesce(T value1, T value2) {
        return value1 != null ? value1 : value2;
    }

    /**
     * Send test email
     */
    public void sendTestEmail(UUID tenantId, TestEmailRequest request) {
        // Implementation would use JavaMailSender or similar
        // For now, just log the request
        System.out.println("Sending test email to: " + request.getTo() +
                           "from tenant: " + tenantId);
    }

    /**
     * Validate IdP configuration
     */
    public IdpValidationResponse validateIdpConfiguration(IdpValidationRequest request) {
        IdpValidationResponse response = new IdpValidationResponse();
        
        // Validate issuer URL format
        if (request.getIssuerUrl() == null || !request.getIssuerUrl().toString().matches("^https?://.*")) {
            response.setValid(false);
            response.setError("Issuer URL must be a valid HTTPS URL");
            return response;
        }
        
        // Validate client ID
        if (request.getClientId() == null || request.getClientId().isBlank()) {
            response.setValid(false);
            response.setError("Client ID is required");
            return response;
        }
        
        // Set validated values
        response.setValid(true);
        response.setIssuer(request.getIssuerUrl().toString());
        // In a real implementation, we would fetch JWKS URI and user info endpoint
        // from issuer URL's .well-known/openid-configuration
        response.setJwksUri(request.getIssuerUrl().toString() + "/.well-known/openid-configuration");
        response.setUserinfoEndpoint(request.getIssuerUrl().toString() + "/userinfo");
        
        return response;
    }
}
