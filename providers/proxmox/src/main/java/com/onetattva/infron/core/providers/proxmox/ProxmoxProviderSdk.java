package com.onetattva.infron.core.providers.proxmox;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.core.providers.*;
import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Provider-SDK implementation for PROXMOX provider-level operations.
 * <p>
 * This intentionally does not modify {@code ProxmoxVmProvider}; provider-level APIs are isolated here.
 */
public class ProxmoxProviderSdk implements ProviderSdk {

    private static final Logger logger = LoggerFactory.getLogger(ProxmoxProviderSdk.class);
    private static final Pattern PASSWORD_PARAM = Pattern.compile("(?i)(password=)[^&\\s]+");
    private static final Pattern API_TOKEN_PARAM = Pattern.compile("(?i)(apiToken=)[^&\\s]+");

    @Override
    public ProviderType providerType() {
        return ProviderType.PROXMOX;
    }

    @Override
    public ProviderSdkConnectionTestResult testConnection(ProviderConnectionInfo connectionInfo) {
        long startTime = System.currentTimeMillis();
        try {
            validateCredentials(connectionInfo);
            validateEndpoint(connectionInfo);

            URI uri = URI.create(connectionInfo.endpoint());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Unable to parse Proxmox host from endpoint");
            }
            int port = uri.getPort() != -1 ? uri.getPort() : 8006;

            Map<String, String> credentials = connectionInfo.credentials();
            String username = credentials.get("username");
            String password = credentials.get("password");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                throw new IllegalArgumentException("Proxmox username and password must be non-empty");
            }
            String realm = credentials.get("realm");
            if (realm == null || realm.isBlank()) {
                realm = "pam";
            }

            // Accept username provided as "user@realm" and split it for pve4j auth params.
            int atIndex = username.indexOf('@');
            if (atIndex > 0 && atIndex < username.length() - 1) {
                realm = username.substring(atIndex + 1);
                username = username.substring(0, atIndex);
            }

            Proxmox proxmox = Proxmox.createWithPassword(
                host,
                port,
                username,
                password,
                realm,
                SecurityConfig.insecure()
            );

            // Read-only call to verify auth + connectivity.
            proxmox.getNodes().getIndex().execute();

            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            return new ProviderSdkConnectionTestResult(
                true,
                "Successfully connected to provider",
                latencyMs,
                getCapabilities(connectionInfo)
            );
        } catch (Exception e) {
            int latencyMs = (int) (System.currentTimeMillis() - startTime);
            String sanitizedMessage = sanitizeSensitiveMessage(e.getMessage());
            logger.warn("Proxmox connection test failed: {}", sanitizedMessage);
            return new ProviderSdkConnectionTestResult(
                false,
                "Connection failed: " + sanitizedMessage,
                latencyMs,
                null
            );
        }
    }

    @Override
    public Map<String, String> getCapabilities(ProviderConnectionInfo connectionInfo) {
        // For now, keep behavior aligned with core-services defaults.
        // (Live capability discovery is done later via orchestrator/providers logic.)
        return getDefaultCapabilities();
    }

    private void validateEndpoint(ProviderConnectionInfo connectionInfo) {
        String endpoint = connectionInfo.endpoint();
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Proxmox endpoint is required");
        }
        if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
            throw new IllegalArgumentException("Proxmox endpoint must be HTTP/HTTPS URL");
        }
    }

    private void validateCredentials(ProviderConnectionInfo connectionInfo) {
        Map<String, String> credentials = connectionInfo.credentials();
        if (credentials == null || credentials.isEmpty()) {
            throw new IllegalArgumentException("Credentials are required");
        }
        if (!credentials.containsKey("username") || !credentials.containsKey("password")) {
            throw new IllegalArgumentException("Proxmox requires username and password");
        }
    }

    private Map<String, String> getDefaultCapabilities() {
        Map<String, String> defaults = new java.util.HashMap<>();
        defaults.put("supportedCpuTypes", "kvm64,host");
        defaults.put("supportedStorageClasses", "local,nfs");
        defaults.put("supportedNetworkTypes", "bridge,ovs");
        defaults.put("supportedOsTypes", "linux,windows");
        defaults.put("maxCpus", "1000");
        // Note: core-services currently checks for "maxMemoryGb" but defaults are set as "maxMemoryMb".
        // Keeping the exact keys avoids changing existing behavior.
        defaults.put("maxMemoryMb", "409600");
        defaults.put("maxStorageGb", "20000");
        defaults.put("maxVms", "500");
        return defaults;
    }

    private String sanitizeSensitiveMessage(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        String sanitized = PASSWORD_PARAM.matcher(message).replaceAll("$1***");
        sanitized = API_TOKEN_PARAM.matcher(sanitized).replaceAll("$1***");
        return sanitized;
    }
}

