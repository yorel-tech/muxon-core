package com.krito.muxon.console;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "infron.console")
public class ConsoleProxyProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:4000",
            "http://127.0.0.1:4000",
            "http://localhost:3000",
            "http://127.0.0.1:3000"));

    private long cleanupIntervalMs = 60_000L;

    /**
     * When true, trust all TLS certificates when connecting to hypervisor VNC (e.g. Proxmox). Use only in dev/lab.
     */
    private boolean trustAllHypervisorTls = true;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public boolean isTrustAllHypervisorTls() {
        return trustAllHypervisorTls;
    }

    public void setTrustAllHypervisorTls(boolean trustAllHypervisorTls) {
        this.trustAllHypervisorTls = trustAllHypervisorTls;
    }
}
