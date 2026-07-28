package com.scal.muxon.db.model;

/**
 * Metadata configuration for SAML 2.0 identity providers
 */
public class Saml2Metadata {

    // Required SAML fields
    private String entityId;
    private String singleSignOnServiceUrl;
    private String singleLogoutServiceUrl;
    private String signingCertificate;
    private String encryptionCertificate;

    // Optional SAML fields
    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
    private String authnContextClassRef = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
    private String signatureAlgorithm = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    // Service Provider configuration
    private String spEntityId;
    private String spAssertionConsumerServiceUrl;
    private String spSingleLogoutServiceUrl;

    // Certificate and key configuration
    private String privateKey;
    private String spSigningCertificate;
    private String spEncryptionCertificate;

    // User attribute mapping
    private String usernameAttribute = "urn:oid:0.9.2342.19200300.100.1.1"; // uid
    private String emailAttribute = "urn:oid:1.3.6.1.4.1.5923.1.1.1.6"; // eduPersonPrincipalName or mail
    private String displayNameAttribute = "urn:oid:2.16.840.1.113730.3.1.241"; // displayName
    private String groupsAttribute = "urn:oid:1.3.6.1.4.1.5923.1.5.1.1"; // eduPersonAffiliation

    // SAML request/response configuration
    private Boolean signRequests = true;
    private Boolean signMetadata = true;
    private Boolean wantAssertionsSigned = true;
    private Boolean wantRequestsSigned = false;
    private Integer maxAuthenticationAge = 7200; // 2 hours in seconds
    private Integer sessionTimeout = 28800; // 8 hours in seconds

    // Relay state configuration
    private String relayStateUrl;
    private Boolean useRelayState = true;

    // Additional SAML settings
    private String metadataUrl;
    private String logoutRequestUrl;
    private String logoutResponseUrl;
    private String artifactResolutionServiceUrl;

    // Provider-specific settings
    private String providerName;
    private String organizationName;
    private String organizationDisplayName;
    private String organizationUrl;

    // Clock skew tolerance
    private Integer clockSkewSeconds = 300; // 5 minutes

    // Response validation
    private Boolean validateSignature = true;
    private Boolean validateInResponseTo = true;
    private Boolean validateNotBefore = true;
    private Boolean validateNotOnOrAfter = true;

    // Constructors
    public Saml2Metadata() {
    }

    public Saml2Metadata(String entityId, String singleSignOnServiceUrl, String signingCertificate) {
        this.entityId = entityId;
        this.singleSignOnServiceUrl = singleSignOnServiceUrl;
        this.signingCertificate = signingCertificate;
    }

    // Getters and setters
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getSingleSignOnServiceUrl() {
        return singleSignOnServiceUrl;
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        this.singleSignOnServiceUrl = singleSignOnServiceUrl;
    }

    public String getSingleLogoutServiceUrl() {
        return singleLogoutServiceUrl;
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        this.singleLogoutServiceUrl = singleLogoutServiceUrl;
    }

    public String getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(String signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public String getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public void setEncryptionCertificate(String encryptionCertificate) {
        this.encryptionCertificate = encryptionCertificate;
    }

    public String getNameIdFormat() {
        return nameIdFormat;
    }

    public void setNameIdFormat(String nameIdFormat) {
        this.nameIdFormat = nameIdFormat;
    }

    public String getAuthnContextClassRef() {
        return authnContextClassRef;
    }

    public void setAuthnContextClassRef(String authnContextClassRef) {
        this.authnContextClassRef = authnContextClassRef;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public String getSpEntityId() {
        return spEntityId;
    }

    public void setSpEntityId(String spEntityId) {
        this.spEntityId = spEntityId;
    }

    public String getSpAssertionConsumerServiceUrl() {
        return spAssertionConsumerServiceUrl;
    }

    public void setSpAssertionConsumerServiceUrl(String spAssertionConsumerServiceUrl) {
        this.spAssertionConsumerServiceUrl = spAssertionConsumerServiceUrl;
    }

    public String getSpSingleLogoutServiceUrl() {
        return spSingleLogoutServiceUrl;
    }

    public void setSpSingleLogoutServiceUrl(String spSingleLogoutServiceUrl) {
        this.spSingleLogoutServiceUrl = spSingleLogoutServiceUrl;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getSpSigningCertificate() {
        return spSigningCertificate;
    }

    public void setSpSigningCertificate(String spSigningCertificate) {
        this.spSigningCertificate = spSigningCertificate;
    }

    public String getSpEncryptionCertificate() {
        return spEncryptionCertificate;
    }

    public void setSpEncryptionCertificate(String spEncryptionCertificate) {
        this.spEncryptionCertificate = spEncryptionCertificate;
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

    public Boolean getSignRequests() {
        return signRequests;
    }

    public void setSignRequests(Boolean signRequests) {
        this.signRequests = signRequests;
    }

    public Boolean getSignMetadata() {
        return signMetadata;
    }

    public void setSignMetadata(Boolean signMetadata) {
        this.signMetadata = signMetadata;
    }

    public Boolean getWantAssertionsSigned() {
        return wantAssertionsSigned;
    }

    public void setWantAssertionsSigned(Boolean wantAssertionsSigned) {
        this.wantAssertionsSigned = wantAssertionsSigned;
    }

    public Boolean getWantRequestsSigned() {
        return wantRequestsSigned;
    }

    public void setWantRequestsSigned(Boolean wantRequestsSigned) {
        this.wantRequestsSigned = wantRequestsSigned;
    }

    public Integer getMaxAuthenticationAge() {
        return maxAuthenticationAge;
    }

    public void setMaxAuthenticationAge(Integer maxAuthenticationAge) {
        this.maxAuthenticationAge = maxAuthenticationAge;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getRelayStateUrl() {
        return relayStateUrl;
    }

    public void setRelayStateUrl(String relayStateUrl) {
        this.relayStateUrl = relayStateUrl;
    }

    public Boolean getUseRelayState() {
        return useRelayState;
    }

    public void setUseRelayState(Boolean useRelayState) {
        this.useRelayState = useRelayState;
    }

    public String getMetadataUrl() {
        return metadataUrl;
    }

    public void setMetadataUrl(String metadataUrl) {
        this.metadataUrl = metadataUrl;
    }

    public String getLogoutRequestUrl() {
        return logoutRequestUrl;
    }

    public void setLogoutRequestUrl(String logoutRequestUrl) {
        this.logoutRequestUrl = logoutRequestUrl;
    }

    public String getLogoutResponseUrl() {
        return logoutResponseUrl;
    }

    public void setLogoutResponseUrl(String logoutResponseUrl) {
        this.logoutResponseUrl = logoutResponseUrl;
    }

    public String getArtifactResolutionServiceUrl() {
        return artifactResolutionServiceUrl;
    }

    public void setArtifactResolutionServiceUrl(String artifactResolutionServiceUrl) {
        this.artifactResolutionServiceUrl = artifactResolutionServiceUrl;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationDisplayName() {
        return organizationDisplayName;
    }

    public void setOrganizationDisplayName(String organizationDisplayName) {
        this.organizationDisplayName = organizationDisplayName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
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

    public Boolean getValidateInResponseTo() {
        return validateInResponseTo;
    }

    public void setValidateInResponseTo(Boolean validateInResponseTo) {
        this.validateInResponseTo = validateInResponseTo;
    }

    public Boolean getValidateNotBefore() {
        return validateNotBefore;
    }

    public void setValidateNotBefore(Boolean validateNotBefore) {
        this.validateNotBefore = validateNotBefore;
    }

    public Boolean getValidateNotOnOrAfter() {
        return validateNotOnOrAfter;
    }

    public void setValidateNotOnOrAfter(Boolean validateNotOnOrAfter) {
        this.validateNotOnOrAfter = validateNotOnOrAfter;
    }
}
