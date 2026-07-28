package com.scal.muxon.customization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GuestUser {
    private String name;
    private String gecos;
    private List<String> groups;
    private String shell = "/bin/bash";
    private List<String> sshAuthorizedKeys;
    /** Decrypted password; held in-memory only during rendering. */
    private String password;
    private String sudo;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGecos() { return gecos; }
    public void setGecos(String gecos) { this.gecos = gecos; }

    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups) { this.groups = groups; }

    public String getShell() { return shell; }
    public void setShell(String shell) { this.shell = shell; }

    public List<String> getSshAuthorizedKeys() { return sshAuthorizedKeys; }
    public void setSshAuthorizedKeys(List<String> keys) { this.sshAuthorizedKeys = keys; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSudo() { return sudo; }
    public void setSudo(String sudo) { this.sudo = sudo; }
}
