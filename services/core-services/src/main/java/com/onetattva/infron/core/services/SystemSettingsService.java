package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.common.Constants;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.db.model.SystemSettingsEntity;
import com.onetattva.infron.db.repository.SystemSettingsRepository;
import com.onetattva.infron.db.repository.IdentityProviderRepository;
import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.core.config.ConfigDecryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    @Autowired
    private ConfigDecryptor configDecryptor;

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
        
        SystemSettingsEntity entity = existing.orElseGet();
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
        
        SystemSettingsEntity entity = existing.orElseGet();
        updateEntityFromDto(entity, settings);
        entity.setTenantId(tenantId);
        entity.setUpdatedAt(Instant.now());
        
        SystemSettingsEntity saved = settingsRepository.save(entity);
        return mapEntityToDto(saved, false);
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
        
        return merged;
    }

    /**
     * Map entity to DTO
     * @param maskSensitive - if true, mask sensitive fields (passwords)
     */
    private SystemSettings mapEntityToDto(SystemSettingsEntity entity, boolean maskSensitive) {
        SystemSettings dto = new SystemSettings();
        
        // General Settings
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setDefaultTimezone(entity.getDefaultTimezone());
        dto.setDefaultLocale(entity.getDefaultLocale());
        
        // Security Settings
        dto.setSessionTimeoutMinutes(entity.getSessionTimeoutMinutes());
        dto.setMaxLoginAttempts(entity.getMaxLoginAttempts());
        dto.setLockoutDurationMinutes(entity.getLockoutDurationMinutes());
        dto.setPasswordMinLength(entity.getPasswordMinLength());
        dto.setPasswordRequireUppercase(entity.getPasswordRequireUppercase());
        dto.setPasswordRequireLowercase(entity.getPasswordRequireLowercase());
        dto.setPasswordRequireDigit(entity.getPasswordRequireDigit());
        dto.setPasswordRequireSymbol(entity.getPasswordRequireSymbol());
        dto.setPasswordExpiryDays(entity.getPasswordExpiryDays());
        dto.setApiRateLimitPerMinute(entity.getApiRateLimitPerMinute());
        
        // Notification Settings - mask password if requested
        if (maskSensitive && entity.getSmtpPassword() != null) {
            dto.setSmtpPassword("*****");
        } else {
            dto.setSmtpPassword(entity.getSmtpPassword());
        }
        dto.setSmtpEnabled(entity.getSmtpEnabled());
        dto.setSmtpHost(entity.getSmtpHost());
        dto.setSmtpPort(entity.getSmtpPort());
        dto.setSmtpUsername(entity.getSmtpUsername());
        dto.setSmtpFromEmail(entity.getSmtpFromEmail());
        dto.setSmtpUseTls(entity.getSmtpUseTls());
        dto.setSlackWebhookUrl(entity.getSlackWebhookUrl());
        dto.setWebhookEnabled(entity.getWebhookEnabled());
        
        // Appearance Settings
        dto.setTheme(entity.getTheme());
        dto.setLogoUrl(entity.getLogoUrl());
        dto.setFaviconUrl(entity.getFaviconUrl());
        dto.setPrimaryColor(entity.getPrimaryColor());
        dto.setSecondaryColor(entity.getSecondaryColor());
        dto.setCustomCssUrl(entity.getCustomCssUrl());
        
        // Audit fields
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        
        return dto;
    }

    /**
     * Update entity from DTO
     * Encrypt sensitive fields before saving
     */
    private void updateEntityFromDto(SystemSettingsEntity entity, SystemSettings dto) {
        // General Settings
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getContactEmail() != null) entity.setContactEmail(dto.getContactEmail());
        if (dto.getContactPhone() != null) entity.setContactPhone(dto.getContactPhone());
        if (dto.getDefaultTimezone() != null) entity.setDefaultTimezone(dto.getDefaultTimezone());
        if (dto.getDefaultLocale() != null) entity.setDefaultLocale(dto.getDefaultLocale());
        
        // Security Settings
        if (dto.getSessionTimeoutMinutes() != null) entity.setSessionTimeoutMinutes(dto.getSessionTimeoutMinutes());
        if (dto.getMaxLoginAttempts() != null) entity.setMaxLoginAttempts(dto.getMaxLoginAttempts());
        if (dto.getLockoutDurationMinutes() != null) entity.setLockoutDurationMinutes(dto.getLockoutDurationMinutes());
        if (dto.getPasswordMinLength() != null) entity.setPasswordMinLength(dto.getPasswordMinLength());
        if (dto.getPasswordRequireUppercase() != null) entity.setPasswordRequireUppercase(dto.getPasswordRequireUppercase());
        if (dto.getPasswordRequireLowercase() != null) entity.setPasswordRequireLowercase(dto.getPasswordRequireLowercase());
        if (dto.getPasswordRequireDigit() != null) entity.setPasswordRequireDigit(dto.getPasswordRequireDigit());
        if (dto.getPasswordRequireSymbol() != null) entity.setPasswordRequireSymbol(dto.getPasswordRequireSymbol());
        if (dto.getPasswordExpiryDays() != null) entity.setPasswordExpiryDays(dto.getPasswordExpiryDays());
        if (dto.getApiRateLimitPerMinute() != null) entity.setApiRateLimitPerMinute(dto.getApiRateLimitPerMinute());
        
        // Notification Settings - encrypt password if provided and not masked
        if (dto.getSmtpPassword() != null && !"*****".equals(dto.getSmtpPassword())) {
            String encrypted = configDecryptor.encrypt(dto.getSmtpPassword());
            entity.setSmtpPassword(encrypted);
        }
        if (dto.getSmtpEnabled() != null) entity.setSmtpEnabled(dto.getSmtpEnabled());
        if (dto.getSmtpHost() != null) entity.setSmtpHost(dto.getSmtpHost());
        if (dto.getSmtpPort() != null) entity.setSmtpPort(dto.getSmtpPort());
        if (dto.getSmtpUsername() != null) entity.setSmtpUsername(dto.getSmtpUsername());
        if (dto.getSmtpFromEmail() != null) entity.setSmtpFromEmail(dto.getSmtpFromEmail());
        if (dto.getSmtpUseTls() != null) entity.setSmtpUseTls(dto.getSmtpUseTls());
        if (dto.getSlackWebhookUrl() != null) entity.setSlackWebhookUrl(dto.getSlackWebhookUrl());
        if (dto.getWebhookEnabled() != null) entity.setWebhookEnabled(dto.getWebhookEnabled());
        
        // Appearance Settings
        if (dto.getTheme() != null) entity.setTheme(dto.getTheme());
        if (dto.getLogoUrl() != null) entity.setLogoUrl(dto.getLogoUrl());
        if (dto.getFaviconUrl() != null) entity.setFaviconUrl(dto.getFaviconUrl());
        if (dto.getPrimaryColor() != null) entity.setPrimaryColor(dto.getPrimaryColor());
        if (dto.getSecondaryColor() != null) entity.setSecondaryColor(dto.getSecondaryColor());
        if (dto.getCustomCssUrl() != null) entity.setCustomCssUrl(dto.getCustomCssUrl());
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
        if (request.getIssuerUrl() == null || !request.getIssuerUrl().matches("^https?://.*")) {
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
        response.setIssuer(request.getIssuerUrl());
        // In a real implementation, we would fetch JWKS URI and user info endpoint
        // from the issuer URL's .well-known/openid-configuration
        response.setJwksUri(request.getIssuerUrl() + "/.well-known/openid-configuration");
        response.setUserinfoEndpoint(request.getIssuerUrl() + "/userinfo");
        
        return response;
    }
}
