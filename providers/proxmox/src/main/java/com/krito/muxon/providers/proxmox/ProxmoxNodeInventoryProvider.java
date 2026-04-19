package com.krito.muxon.providers.proxmox;

import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import fr.freshperf.pve4j.entities.cluster.PveClusterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Discovers Proxmox cluster nodes for inventory sync (node + cluster rows in the control plane).
 */
public final class ProxmoxNodeInventoryProvider {

    private static final Logger log = LoggerFactory.getLogger(ProxmoxNodeInventoryProvider.class);

    public record DiscoveredNode(String name, int maxCpu, long maxMemBytes, String rawStatus) {
    }

    /**
     * Nodes plus optional Proxmox cluster name from {@code /cluster/status} (row with type {@code cluster}).
     */
    public record InventorySnapshot(List<DiscoveredNode> nodes, Optional<String> clusterName) {
    }

    public List<DiscoveredNode> discoverNodes(Map<String, Object> connectionInfo) {
        return discoverInventory(connectionInfo).nodes();
    }

    public InventorySnapshot discoverInventory(Map<String, Object> connectionInfo) {
        try {
            Proxmox proxmox = createProxmoxClient(connectionInfo);
            Optional<String> clusterName = readProxmoxClusterName(proxmox);
            List<DiscoveredNode> nodes = readNodes(proxmox);
            return new InventorySnapshot(nodes, clusterName);
        } catch (Exception e) {
            throw new RuntimeException("Proxmox node discovery failed: " + e.getMessage(), e);
        }
    }

    private static Optional<String> readProxmoxClusterName(Proxmox proxmox) {
        try {
            List<PveClusterStatus> statuses = proxmox.getCluster().getStatus().execute();
            if (statuses == null) {
                return Optional.empty();
            }
            for (PveClusterStatus row : statuses) {
                if (row.getType() == PveClusterStatus.Type.cluster) {
                    String n = row.getName();
                    if (n != null && !n.isBlank()) {
                        return Optional.of(n.trim());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Proxmox cluster status not available for cluster name (standalone or API error): {}",
                    e.getMessage());
        }
        return Optional.empty();
    }

    private static List<DiscoveredNode> readNodes(Proxmox proxmox) throws Exception {
        List<DiscoveredNode> out = new ArrayList<>();
        Object nodesApi = invokeMethod(proxmox, "getNodes");
        Object nodesIndexApi = invokeMethod(nodesApi, "getIndex");
        Object nodesResult = invokeMethod(nodesIndexApi, "execute");
        if (!(nodesResult instanceof List<?> nodes)) {
            return out;
        }
        for (Object nodeObj : nodes) {
            String nodeName = readString(nodeObj, "getNode");
            if (nodeName == null || nodeName.isBlank()) {
                continue;
            }
            int maxCpu = readInt(nodeObj, 0, "getMaxcpu", "getMaxCpu");
            long maxMem = readLong(nodeObj, 0L, "getMaxmem", "getMaxMem");
            String status = readString(nodeObj, "getStatus");
            out.add(new DiscoveredNode(nodeName, maxCpu, maxMem, status));
        }
        return out;
    }

    private Proxmox createProxmoxClient(Map<String, Object> connectionInfo) {
        String endpoint = (String) connectionInfo.get("endpoint");
        @SuppressWarnings("unchecked")
        Map<String, String> credentials = (Map<String, String>) connectionInfo.get("credentials");
        if (endpoint == null || credentials == null) {
            throw new IllegalArgumentException("Missing endpoint or credentials");
        }
        URI uri = URI.create(endpoint);
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 8006;
        String username = credentials.get("username");
        String password = credentials.get("password");
        String realm = credentials.getOrDefault("realm", "pam");
        if (username != null && username.contains("@")) {
            int atIndex = username.indexOf('@');
            realm = username.substring(atIndex + 1);
            username = username.substring(0, atIndex);
        }
        try {
            return Proxmox.createWithPassword(host, port, username, password, realm, SecurityConfig.insecure());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Proxmox client", e);
        }
    }

    private static Object invokeMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static String readString(Object source, String getterName) {
        try {
            Method method = source.getClass().getMethod(getterName);
            Object value = method.invoke(source);
            return value != null ? String.valueOf(value).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static long readLong(Object source, long defaultVal, String... getterNames) {
        for (String getter : getterNames) {
            try {
                Method method = source.getClass().getMethod(getter);
                Object value = method.invoke(source);
                if (value instanceof Number) {
                    return ((Number) value).longValue();
                }
                if (value != null) {
                    return Long.parseLong(String.valueOf(value));
                }
            } catch (Exception ignored) {
            }
        }
        return defaultVal;
    }

    private static int readInt(Object source, int defaultVal, String... getterNames) {
        for (String getter : getterNames) {
            try {
                Method method = source.getClass().getMethod(getter);
                Object value = method.invoke(source);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                }
                if (value != null) {
                    return Integer.parseInt(String.valueOf(value));
                }
            } catch (Exception ignored) {
            }
        }
        return defaultVal;
    }
}
