package com.scal.muxon.db.model;

import java.util.List;

/**
 * Metadata configuration for OAuth2.0 identity providers
 */
public class OAuth2Metadata {

    // Required OAuth2 fields
    private String clientId;
    private String clientSecret;
    private String authorizationUri;
    private String tokenUri;
    private List<String> scope;
    private String grantType = "authorization_code";

    // Optional OAuth2 endpoints
    private String userInfoUri;
    private String userAuthorizationUri;
    private String accessTokenUri;
    private String checkTokenUri;
    private String logoutUri;

    // User attribute mapping
    private String usernameAttribute = "username";
    private String emailAttribute = "email";
    private String displayNameAttribute = "name";
    private String groupsAttribute = "groups";

    // Token configuration
    private String tokenName = "access_token";
    private String tokenType = "Bearer";
    private Boolean useParametersForClientAuthentication = false;

    // PKCE configuration
    private Boolean usePkce = false;
    private String pkceMethod = "S256";

    // Additional configuration
    private String clientAuthenticationMethod = "client_secret_basic";
    private String userNameAttributeName;
    private String clientName = "Infron";

    // Provider-specific settings
    private String providerId;
    private String registrationId;
    private String redirectUriTemplate = "{baseUrl}/login/oauth2/code/{registrationId}";

    // Constructors
    public OAuth2Metadata() {
    }

    public OAuth2Metadata(String clientId, String clientSecret, String authorizationUri, String tokenUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
    }

    // Getters and setters
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

    public String getAuthorizationUri() {
        return authorizationUri;
    }

    public void setAuthorizationUri(String authorizationUri) {
        this.authorizationUri = authorizationUri;
    }

    public String getTokenUri() {
        return tokenUri;
    }

    public void setTokenUri(String tokenUri) {
        this.tokenUri = tokenUri;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(List<String> scope) {
        this.scope = scope;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public void setUserInfoUri(String userInfoUri) {
        this.userInfoUri = userInfoUri;
    }

    public String getUserAuthorizationUri() {
        return userAuthorizationUri;
    }

    public void setUserAuthorizationUri(String userAuthorizationUri) {
        this.userAuthorizationUri = userAuthorizationUri;
    }

    public String getAccessTokenUri() {
        return accessTokenUri;
    }

    public void setAccessTokenUri(String accessTokenUri) {
        this.accessTokenUri = accessTokenUri;
    }

    public String getCheckTokenUri() {
        return checkTokenUri;
    }

    public void setCheckTokenUri(String checkTokenUri) {
        this.checkTokenUri = checkTokenUri;
    }

    public String getLogoutUri() {
        return logoutUri;
    }

    public void setLogoutUri(String logoutUri) {
        this.logoutUri = logoutUri;
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

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public Boolean getUseParametersForClientAuthentication() {
        return useParametersForClientAuthentication;
    }

    public void setUseParametersForClientAuthentication(Boolean useParametersForClientAuthentication) {
        this.useParametersForClientAuthentication = useParametersForClientAuthentication;
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

    public String getClientAuthenticationMethod() {
        return clientAuthenticationMethod;
    }

    public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
        this.clientAuthenticationMethod = clientAuthenticationMethod;
    }

    public String getUserNameAttributeName() {
        return userNameAttributeName;
    }

    public void setUserNameAttributeName(String userNameAttributeName) {
        this.userNameAttributeName = userNameAttributeName;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getRedirectUriTemplate() {
        return redirectUriTemplate;
    }

    public void setRedirectUriTemplate(String redirectUriTemplate) {
        this.redirectUriTemplate = redirectUriTemplate;
    }
}
