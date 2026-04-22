package com.krito.muxon.customization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WindowsCustomizationSpec {
    /** Decrypted; held in-memory only during rendering. */
    private String adminPassword;
    private String productKey;
    private WindowsDomainJoin joinDomain;
    private String workgroup;
    private String uiLanguage = "en-US";
    private String systemLocale = "en-US";
    private String userLocale = "en-US";
    private String inputLocale = "0409:00000409";
    private int autoLogonCount = 0;
    private List<String> firstLogonCommands;
    /** Resolved script content to embed in specialize pass. */
    private List<String> resolvedPreScripts;
    /** Resolved script content to embed as firstLogonCommands. */
    private List<String> resolvedPostScripts;

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public String getProductKey() { return productKey; }
    public void setProductKey(String productKey) { this.productKey = productKey; }

    public WindowsDomainJoin getJoinDomain() { return joinDomain; }
    public void setJoinDomain(WindowsDomainJoin joinDomain) { this.joinDomain = joinDomain; }

    public String getWorkgroup() { return workgroup; }
    public void setWorkgroup(String workgroup) { this.workgroup = workgroup; }

    public String getUiLanguage() { return uiLanguage; }
    public void setUiLanguage(String uiLanguage) { this.uiLanguage = uiLanguage; }

    public String getSystemLocale() { return systemLocale; }
    public void setSystemLocale(String systemLocale) { this.systemLocale = systemLocale; }

    public String getUserLocale() { return userLocale; }
    public void setUserLocale(String userLocale) { this.userLocale = userLocale; }

    public String getInputLocale() { return inputLocale; }
    public void setInputLocale(String inputLocale) { this.inputLocale = inputLocale; }

    public int getAutoLogonCount() { return autoLogonCount; }
    public void setAutoLogonCount(int autoLogonCount) { this.autoLogonCount = autoLogonCount; }

    public List<String> getFirstLogonCommands() { return firstLogonCommands; }
    public void setFirstLogonCommands(List<String> firstLogonCommands) { this.firstLogonCommands = firstLogonCommands; }

    public List<String> getResolvedPreScripts() { return resolvedPreScripts; }
    public void setResolvedPreScripts(List<String> resolvedPreScripts) { this.resolvedPreScripts = resolvedPreScripts; }

    public List<String> getResolvedPostScripts() { return resolvedPostScripts; }
    public void setResolvedPostScripts(List<String> resolvedPostScripts) { this.resolvedPostScripts = resolvedPostScripts; }
}
