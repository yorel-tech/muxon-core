package com.krito.muxon.customization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WindowsDomainJoin {
    private String domain;
    private String username;
    /** Decrypted; held in-memory only during rendering. */
    private String password;
    private String ou;

    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getOu() { return ou; }
    public void setOu(String ou) { this.ou = ou; }
}
