package com.onetattva.infron.db.model;

import com.onetattva.infron.core.common.Constants;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for system_settings table.
 * Supports both system-level defaults (tenant_id = SYSTEM_ID)
 * and tenant-specific overrides (tenant_id = tenant UUID).
 */
@Entity
@Table(name = "system_settings")
public class SystemSettingsEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    // General Settings
    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "default_timezone")
    private String defaultTimezone;

    @Column(name = "default_locale")
    private String defaultLocale;

    // Security Settings
    @Column(name = "session_timeout_minutes")
    private Integer sessionTimeoutMinutes;

    @Column(name = "max_login_attempts")
    private Integer maxLoginAttempts;

    @Column(name = "lockout_duration_minutes")
    private Integer lockoutDurationMinutes;

    @Column(name = "password_min_length")
    private Integer passwordMinLength;

    @Column(name = "password_require_uppercase")
    private Boolean passwordRequireUppercase;

    @Column(name = "password_require_lowercase")
    private Boolean passwordRequireLowercase;

    @Column(name = "password_require_digit")
    private Boolean passwordRequireDigit;

    @Column(name = "password_require_symbol")
    private Boolean passwordRequireSymbol;

    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays;

    @Column(name = "api_rate_limit_per_minute")
    private Integer apiRateLimitPerMinute;

    @Column(name = "console_session_timeout_minutes")
    private Integer consoleSessionTimeoutMinutes;

    // Notification Settings
    @Column(name = "smtp_enabled")
    private Boolean smtpEnabled;

    @Column(name = "smtp_host")
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "smtp_from_email")
    private String smtpFromEmail;

    @Column(name = "smtp_use_tls")
    private Boolean smtpUseTls;

    @Column(name = "slack_webhook_url")
    private String slackWebhookUrl;

    @Column(name = "webhook_enabled")
    private Boolean webhookEnabled;

    // Appearance Settings
    @Column(name = "theme")
    private String theme;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "secondary_color")
    private String secondaryColor;

    @Column(name = "custom_css_url")
    private String customCssUrl;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Constructors
    public SystemSettingsEntity() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getDefaultTimezone() {
        return defaultTimezone;
    }

    public void setDefaultTimezone(String defaultTimezone) {
        this.defaultTimezone = defaultTimezone;
    }

    public String getDefaultLocale() {
        return defaultLocale;
    }

    public void setDefaultLocale(String defaultLocale) {
        this.defaultLocale = defaultLocale;
    }

    public Integer getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public Integer getMaxLoginAttempts() {
        return maxLoginAttempts;
    }

    public void setMaxLoginAttempts(Integer maxLoginAttempts) {
        this.maxLoginAttempts = maxLoginAttempts;
    }

    public Integer getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    public void setLockoutDurationMinutes(Integer lockoutDurationMinutes) {
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    public Integer getPasswordMinLength() {
        return passwordMinLength;
    }

    public void setPasswordMinLength(Integer passwordMinLength) {
        this.passwordMinLength = passwordMinLength;
    }

    public Boolean getPasswordRequireUppercase() {
        return passwordRequireUppercase;
    }

    public void setPasswordRequireUppercase(Boolean passwordRequireUppercase) {
        this.passwordRequireUppercase = passwordRequireUppercase;
    }

    public Boolean getPasswordRequireLowercase() {
        return passwordRequireLowercase;
    }

    public void setPasswordRequireLowercase(Boolean passwordRequireLowercase) {
        this.passwordRequireLowercase = passwordRequireLowercase;
    }

    public Boolean getPasswordRequireDigit() {
        return passwordRequireDigit;
    }

    public void setPasswordRequireDigit(Boolean passwordRequireDigit) {
        this.passwordRequireDigit = passwordRequireDigit;
    }

    public Boolean getPasswordRequireSymbol() {
        return passwordRequireSymbol;
    }

    public void setPasswordRequireSymbol(Boolean passwordRequireSymbol) {
        this.passwordRequireSymbol = passwordRequireSymbol;
    }

    public Integer getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    public void setPasswordExpiryDays(Integer passwordExpiryDays) {
        this.passwordExpiryDays = passwordExpiryDays;
    }

    public Integer getApiRateLimitPerMinute() {
        return apiRateLimitPerMinute;
    }

    public void setApiRateLimitPerMinute(Integer apiRateLimitPerMinute) {
        this.apiRateLimitPerMinute = apiRateLimitPerMinute;
    }

    public Integer getConsoleSessionTimeoutMinutes() {
        return consoleSessionTimeoutMinutes;
    }

    public void setConsoleSessionTimeoutMinutes(Integer consoleSessionTimeoutMinutes) {
        this.consoleSessionTimeoutMinutes = consoleSessionTimeoutMinutes;
    }

    public Boolean getSmtpEnabled() {
        return smtpEnabled;
    }

    public void setSmtpEnabled(Boolean smtpEnabled) {
        this.smtpEnabled = smtpEnabled;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public Integer getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(Integer smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getSmtpFromEmail() {
        return smtpFromEmail;
    }

    public void setSmtpFromEmail(String smtpFromEmail) {
        this.smtpFromEmail = smtpFromEmail;
    }

    public Boolean getSmtpUseTls() {
        return smtpUseTls;
    }

    public void setSmtpUseTls(Boolean smtpUseTls) {
        this.smtpUseTls = smtpUseTls;
    }

    public String getSlackWebhookUrl() {
        return slackWebhookUrl;
    }

    public void setSlackWebhookUrl(String slackWebhookUrl) {
        this.slackWebhookUrl = slackWebhookUrl;
    }

    public Boolean getWebhookEnabled() {
        return webhookEnabled;
    }

    public void setWebhookEnabled(Boolean webhookEnabled) {
        this.webhookEnabled = webhookEnabled;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getCustomCssUrl() {
        return customCssUrl;
    }

    public void setCustomCssUrl(String customCssUrl) {
        this.customCssUrl = customCssUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }
}
