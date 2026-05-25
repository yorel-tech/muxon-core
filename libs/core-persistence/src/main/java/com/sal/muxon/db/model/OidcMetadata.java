package com.sal.muxon.db.model;

import java.util.List;

/**
 * Metadata configuration for OIDC identity providers
 */
public class OidcMetadata {

    // Required OIDC fields
    private String issuerUri;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;

    // Optional OIDC endpoints (can be discovered from issuerUri)
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;
    private String jwkSetUri;
    private String endSessionEndpoint;

    // Additional configuration
    private Boolean validateIssuer = true;
    private List<String> validAudiences;
    private String usernameAttribute = "preferred_username";
    private String emailAttribute = "email";
    private String displayNameAttribute = "name";
    private String groupsAttribute = "groups";

    // PKCE configuration
    private Boolean usePkce = true;
    private String pkceMethod = "S256";

    // Token validation
    private Integer clockSkewSeconds = 60;
    private Boolean validateSignature = true;

    // Constructors
    public OidcMetadata() {
    }

    public OidcMetadata(String issuerUri, String clientId, String clientSecret, String redirectUri) {
        this.issuerUri = issuerUri;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    // Getters and setters
    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public String getJwkSetUri() {
        return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
        this.jwkSetUri = jwkSetUri;
    }

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    public Boolean getValidateIssuer() {
        return validateIssuer;
    }

    public void setValidateIssuer(Boolean validateIssuer) {
        this.validateIssuer = validateIssuer;
    }

    public List<String> getValidAudiences() {
        return validAudiences;
    }

    public void setValidAudiences(List<String> validAudiences) {
        this.validAudiences = validAudiences;
    }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    public String getEmailAttribute() {
        return emailAttribute;
    }

    public void setEmailAttribute(String emailAttribute) {
        this.emailAttribute = emailAttribute;
    }

    public String getDisplayNameAttribute() {
        return displayNameAttribute;
    }

    public void setDisplayNameAttribute(String displayNameAttribute) {
        this.displayNameAttribute = displayNameAttribute;
    }

    public String getGroupsAttribute() {
        return groupsAttribute;
    }

    public void setGroupsAttribute(String groupsAttribute) {
        this.groupsAttribute = groupsAttribute;
    }

    public Boolean getUsePkce() {
        return usePkce;
    }

    public void setUsePkce(Boolean usePkce) {
        this.usePkce = usePkce;
    }

    public String getPkceMethod() {
        return pkceMethod;
    }

    public void setPkceMethod(String pkceMethod) {
        this.pkceMethod = pkceMethod;
    }

    public Integer getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(Integer clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    public Boolean getValidateSignature() {
        return validateSignature;
    }

    public void setValidateSignature(Boolean validateSignature) {
        this.validateSignature = validateSignature;
    }
}
