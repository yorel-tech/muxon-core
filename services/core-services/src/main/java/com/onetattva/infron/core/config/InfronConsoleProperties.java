package com.onetattva.infron.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Console proxy URL and limits for VM graphical console sessions.
 */
@ConfigurationProperties(prefix = "infron.console")
public class InfronConsoleProperties {

    /**
     * Base WebSocket URL for the console-proxy service (e.g. ws://localhost:8082/ws/console).
     * The session token is appended as a query parameter.
     */
    private String proxyWsBaseUrl = "ws://localhost:8082/ws/console";

    private int maxSessionsPerUser = 5;

    public String getProxyWsBaseUrl() {
        return proxyWsBaseUrl;
    }

    public void setProxyWsBaseUrl(String proxyWsBaseUrl) {
        this.proxyWsBaseUrl = proxyWsBaseUrl;
    }

    public int getMaxSessionsPerUser() {
        return maxSessionsPerUser;
    }

    public void setMaxSessionsPerUser(int maxSessionsPerUser) {
        this.maxSessionsPerUser = maxSessionsPerUser;
    }
}
