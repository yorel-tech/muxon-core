package com.scal.muxon.providers.proxmox;

import com.scal.muxon.api.model.ProviderType;
import com.scal.muxon.providers.*;
import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        try {
            validateCredentials(connectionInfo);
            validateEndpoint(connectionInfo);
            Proxmox proxmox = createClient(connectionInfo);
            return discoverCapabilities(proxmox);
        } catch (Exception e) {
            logger.warn("Falling back to default Proxmox capabilities: {}", sanitizeSensitiveMessage(e.getMessage()));
            return getDefaultCapabilities();
        }
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

    private Proxmox createClient(ProviderConnectionInfo connectionInfo) {
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

        try {
            return Proxmox.createWithPassword(
                host,
                port,
                username,
                password,
                realm,
                SecurityConfig.insecure()
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Proxmox client", e);
        }
    }

    private Map<String, String> discoverCapabilities(Proxmox proxmox) {
        Map<String, String> capabilities = new java.util.HashMap<>(getDefaultCapabilities());
        Set<String> storageTypes = new LinkedHashSet<>();
        Set<String> networkTypes = new LinkedHashSet<>();
        Set<String> cpuTypes = new LinkedHashSet<>();
        List<String> nodeNames = new ArrayList<>();

        long totalMaxCpu = 0L;
        long totalMaxMemBytes = 0L;

        Object nodesApi = invokeNoArgs(proxmox, "getNodes");
        Object nodesIndexApi = invokeNoArgs(nodesApi, "getIndex");
        Object nodesResult = invokeNoArgs(nodesIndexApi, "execute");
        if (!(nodesResult instanceof List<?> nodes)) {
            return capabilities;
        }

        for (Object nodeObj : nodes) {
            String nodeName = readString(nodeObj, "getNode");
            if (nodeName == null || nodeName.isBlank()) {
                continue;
            }
            nodeNames.add(nodeName);

            totalMaxCpu += readLong(nodeObj, 0L, "getMaxcpu", "getMaxCpu");
            totalMaxMemBytes += readLong(nodeObj, 0L, "getMaxmem", "getMaxMem");

            String cpuModel = readString(nodeObj, "getCpu", "getModel");
            if (cpuModel != null && !cpuModel.isBlank()) {
                cpuTypes.add(cpuModel);
            }
        }

        for (String nodeName : nodeNames) {
            Object nodeApi = invokeWithArg(nodesApi, "get", nodeName);

            // Storage discovery: /nodes/{node}/storage
            Object storageApi = tryInvokeNoArgs(nodeApi, "getStorage");
            if (storageApi != null) {
                Object storageIndexApi = tryInvokeNoArgs(storageApi, "getIndex");
                Object storageResult = storageIndexApi != null ? tryInvokeNoArgs(storageIndexApi, "execute") : null;
                if (storageResult instanceof List<?> storageList) {
                    for (Object storageObj : storageList) {
                        String storageType = readString(storageObj, "getType");
                        if (storageType != null && !storageType.isBlank()) {
                            storageTypes.add(storageType);
                        }
                    }
                }
            }

            // Network discovery: /nodes/{node}/network
            Object networkApi = tryInvokeNoArgs(nodeApi, "getNetwork");
            if (networkApi != null) {
                Object networkIndexApi = tryInvokeNoArgs(networkApi, "getIndex");
                Object networkResult = networkIndexApi != null ? tryInvokeNoArgs(networkIndexApi, "execute") : null;
                if (networkResult instanceof List<?> networkList) {
                    for (Object networkObj : networkList) {
                        String networkType = readString(networkObj, "getType");
                        if (networkType != null && !networkType.isBlank()) {
                            networkTypes.add(networkType);
                        }
                    }
                }
            }
        }

        if (!cpuTypes.isEmpty()) {
            capabilities.put("supportedCpuTypes", String.join(",", cpuTypes));
        }
        if (!storageTypes.isEmpty()) {
            capabilities.put("supportedStorageClasses", String.join(",", storageTypes));
        }
        if (!networkTypes.isEmpty()) {
            capabilities.put("supportedNetworkTypes", String.join(",", networkTypes));
        }

        if (totalMaxCpu > 0) {
            capabilities.put("maxCpus", String.valueOf(totalMaxCpu));
        }
        if (totalMaxMemBytes > 0) {
            capabilities.put("maxMemoryMb", String.valueOf(totalMaxMemBytes / (1024L * 1024L)));
        }

        return capabilities;
    }

    private Object invokeNoArgs(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to invoke method " + methodName, e);
        }
    }

    private Object tryInvokeNoArgs(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object invokeWithArg(Object target, String methodName, Object arg) {
        try {
            return target.getClass().getMethod(methodName, arg.getClass()).invoke(target, arg);
        } catch (Exception first) {
            try {
                // Some generated APIs expose `get(String)` with declared String type.
                return target.getClass().getMethod(methodName, String.class).invoke(target, String.valueOf(arg));
            } catch (Exception second) {
                throw new IllegalStateException("Failed to invoke method " + methodName, second);
            }
        }
    }

    private String readString(Object source, String... getterNames) {
        for (String getter : getterNames) {
            Object value = tryInvokeNoArgs(source, getter);
            if (value != null) {
                String str = String.valueOf(value).trim();
                if (!str.isEmpty()) {
                    return str;
                }
            }
        }
        return null;
    }

    private long readLong(Object source, long defaultValue, String... getterNames) {
        for (String getter : getterNames) {
            Object value = tryInvokeNoArgs(source, getter);
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                // Try next getter name.
            }
        }
        return defaultValue;
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

