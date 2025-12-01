package com.onetattva.infron.core.services.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OidcUserInfo {

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    // Constructors
    public OidcUserInfo() {}

    // Getters and setters
    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getPreferredUsername() {
        return preferredUsername;
    }

    public void setPreferredUsername(String preferredUsername) {
        this.preferredUsername = preferredUsername;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
