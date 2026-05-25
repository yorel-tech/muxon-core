package com.sal.muxon.providers.proxmox;

import com.sal.muxon.providers.storage.StorageDiscoveryProvider;
import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Proxmox storage discovery implementation.
 * <p>
 * Discovers storage configurations from Proxmox clusters and normalizes them
 * into the common capability model. This implementation uses the Proxmox API
 * client to connect to clusters and enumerate storage.
 * </p>
 */
public class ProxmoxStorageDiscoveryProvider implements StorageDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(ProxmoxStorageDiscoveryProvider.class);

    // Capability mappings for Proxmox storage types
    private static final Map<String, Map<String, Object>> STORAGE_TYPE_CAPABILITIES = Map.ofEntries(
        Map.entry("rbd", Map.of(
            "performance", "high",
            "media", "ssd",
            "redundancy", "replicated",
            "shared", true
        )),
        Map.entry("zfspool", Map.of(
            "performance", "medium",
            "media", "ssd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("lvmthin", Map.of(
            "performance", "medium",
            "media", "ssd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("lvm", Map.of(
            "performance", "medium",
            "media", "ssd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("dir", Map.of(
            "performance", "low",
            "media", "hdd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("nfs", Map.of(
            "performance", "low",
            "media", "hdd",
            "redundancy", "none",
            "shared", true
        )),
        Map.entry("cifs", Map.of(
            "performance", "low",
            "media", "hdd",
            "redundancy", "none",
            "shared", true
        ))
    );

    @Override
    public String getProviderType() {
        return "proxmox";
    }

    @Override
    public CompletableFuture<List<DiscoveredStorage>> discoverStorage(
            UUID providerId, 
            Map<String, Object> connectionInfo) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<DiscoveredStorage> discoveredStorage = new ArrayList<>();

            log.info("[proxmox-storage] discoverStorage async task started: providerId={}", providerId);

            try {
                Proxmox proxmox = createProxmoxClient(connectionInfo);
                log.info("[proxmox-storage] API client created: providerId={}", providerId);

                // Get all nodes
                Object nodesApi = invokeMethod(proxmox, "getNodes");
                Object nodesIndexApi = invokeMethod(nodesApi, "getIndex");
                Object nodesResult = invokeMethod(nodesIndexApi, "execute");

                if (!(nodesResult instanceof List<?> nodes)) {
                    log.warn("[proxmox-storage] nodes index did not return a list (got {}): providerId={}",
                            nodesResult != null ? nodesResult.getClass().getName() : "null", providerId);
                    return discoveredStorage;
                }

                log.info("[proxmox-storage] cluster reports {} node(s): providerId={}", nodes.size(), providerId);

                Set<String> processedStorage = new HashSet<>();

                for (Object nodeObj : nodes) {
                    String nodeName = readString(nodeObj, "getNode");
                    if (nodeName == null || nodeName.isEmpty()) {
                        log.debug("[proxmox-storage] skipping node entry with empty name: providerId={}", providerId);
                        continue;
                    }

                    // Get storage for this node
                    Object nodeApi = invokeMethodWithArg(nodesApi, "get", nodeName);
                    Object storageApi = tryInvokeMethod(nodeApi, "getStorage");

                    if (storageApi != null) {
                        Object storageIndexApi = tryInvokeMethod(storageApi, "getIndex");
                        Object storageResult = storageIndexApi != null ?
                            tryInvokeMethod(storageIndexApi, "execute") : null;

                        if (storageResult instanceof List<?> storageList) {
                            log.info("[proxmox-storage] node {} returned {} raw storage row(s): providerId={}",
                                    nodeName, storageList.size(), providerId);
                            for (Object storageObj : storageList) {
                                DiscoveredStorage storage = discoverStorageEntry(
                                    storageObj, nodeName, providerId, processedStorage);
                                if (storage != null) {
                                    discoveredStorage.add(storage);
                                }
                            }
                        } else {
                            log.warn("[proxmox-storage] node {} storage index was not a list (got {}): providerId={}",
                                    nodeName, storageResult != null ? storageResult.getClass().getName() : "null", providerId);
                        }
                    } else {
                        log.warn("[proxmox-storage] node {} has no getStorage API: providerId={}", nodeName, providerId);
                    }
                }

                log.info("[proxmox-storage] discovered {} unique storage entries: providerId={}",
                    discoveredStorage.size(), providerId);
                
            } catch (Exception e) {
                log.error("[proxmox-storage] discovery failed for provider {}: {}",
                    providerId, e.getMessage(), e);
            }
            
            return discoveredStorage;
        });
    }

    @Override
    public CompletableFuture<StorageDiscoveryTestResult> testConnection(Map<String, Object> connectionInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Proxmox proxmox = createProxmoxClient(connectionInfo);
                
                // Try to get cluster status
                Object nodesApi = invokeMethod(proxmox, "getNodes");
                Object nodesIndexApi = invokeMethod(nodesApi, "getIndex");
                Object nodesResult = invokeMethod(nodesIndexApi, "execute");
                
                if (nodesResult instanceof List<?> nodes) {
                    Map<String, Object> info = new HashMap<>();
                    info.put("node_count", nodes.size());
                    info.put("cluster_type", "proxmox");
                    
                    return StorageDiscoveryTestResult.success(
                        "Successfully connected to Proxmox cluster with " + nodes.size() + " nodes",
                        info
                    );
                }
                
                return StorageDiscoveryTestResult.failure("Failed to retrieve cluster information");
            } catch (Exception e) {
                return StorageDiscoveryTestResult.failure(
                    "Failed to connect: " + e.getMessage()
                );
            }
        });
    }

    @Override
    public StorageDiscoveryCapabilities getCapabilities() {
        return new StorageDiscoveryCapabilities(
            List.of("rbd", "zfspool", "lvmthin", "lvm", "dir", "nfs", "cifs"),
            true,  // supportsAutoDiscovery
            true,  // supportsMetrics
            true,  // supportsMultiNode
            Map.of(
                "endpoint", "Proxmox API endpoint (e.g., https://proxmox.example.com:8006)",
                "username", "Username for authentication",
                "password", "Password for authentication",
                "realm", "Authentication realm (default: pam)"
            )
        );
    }

    private DiscoveredStorage discoverStorageEntry(
            Object storageObj, String nodeName, UUID providerId, Set<String> processedStorage) {
        
        try {
            String storageId = readString(storageObj, "getStorage");
            String storageType = readString(storageObj, "getType");
            
            if (storageId == null || storageType == null) {
                return null;
            }
            
            // Skip if already processed (storage can be shared across nodes)
            if (processedStorage.contains(storageId)) {
                return null;
            }
            processedStorage.add(storageId);
            
            // Get capabilities for this storage type
            Map<String, Object> capabilities = new HashMap<>(
                STORAGE_TYPE_CAPABILITIES.getOrDefault(storageType, Map.of(
                    "performance", "medium",
                    "media", "unknown",
                    "redundancy", "none",
                    "shared", false
                ))
            );
            
            // Extract metrics
            Map<String, Object> metrics = new HashMap<>();
            Long total = readLong(storageObj, "getTotal");
            Long avail = readLong(storageObj, "getAvail");
            Long used = readLong(storageObj, "getUsed");
            
            if (total != null && total > 0) {
                metrics.put("total_gb", bytesToGb(total));
                metrics.put("free_gb", avail != null ? bytesToGb(avail) : 0);
                metrics.put("used_gb", used != null ? bytesToGb(used) : 0);
            }
            
            // Estimate IOPS based on storage type
            metrics.put("estimated_iops", estimateIops(storageType));

            // Content types accepted by this storage (for upload validation)
            String content = readString(storageObj, "getContent");
            if (content != null && !content.isBlank()) {
                capabilities.put(
                        "content_types",
                        Arrays.stream(content.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty())
                                .toList());
            }
            
            // Check if enabled/active
            Integer active = readInteger(storageObj, "getActive");
            Boolean enabled = readBoolean(storageObj, "getEnabled");
            metrics.put("active", active != null && active > 0);
            metrics.put("enabled", enabled != null ? enabled : true);
            
            log.debug("Discovered Proxmox storage: id={}, type={}, node={}, capabilities={}, metrics={}", 
                storageId, storageType, nodeName, capabilities, metrics);
            
            return new DiscoveredStorage(
                storageId,
                storageId,
                storageType,
                capabilities,
                metrics,
                nodeName
            );
            
        } catch (Exception e) {
            log.warn("Failed to process Proxmox storage entry: {}", e.getMessage());
            return null;
        }
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
        
        // Handle username@realm format
        if (username != null && username.contains("@")) {
            int atIndex = username.indexOf('@');
            realm = username.substring(atIndex + 1);
            username = username.substring(0, atIndex);
        }
        
        try {
            return Proxmox.createWithPassword(host, port, username, password, realm, 
                SecurityConfig.insecure());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Proxmox client", e);
        }
    }

    private long bytesToGb(long bytes) {
        return bytes / (1024L * 1024L * 1024L);
    }

    private int estimateIops(String storageType) {
        // Rough IOPS estimates based on storage type
        return switch (storageType) {
            case "rbd" -> 50000;
            case "zfspool" -> 10000;
            case "lvmthin" -> 8000;
            case "lvm" -> 5000;
            case "dir", "nfs", "cifs" -> 1000;
            default -> 3000;
        };
    }

    // Reflection helper methods
    private Object invokeMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private Object tryInvokeMethod(Object target, String methodName) {
        try {
            return invokeMethod(target, methodName);
        } catch (Exception e) {
            return null;
        }
    }

    private Object invokeMethodWithArg(Object target, String methodName, Object arg) throws Exception {
        try {
            Method method = target.getClass().getMethod(methodName, arg.getClass());
            return method.invoke(target, arg);
        } catch (NoSuchMethodException e) {
            Method method = target.getClass().getMethod(methodName, String.class);
            return method.invoke(target, String.valueOf(arg));
        }
    }

    private String readString(Object source, String getterName) {
        try {
            Method method = source.getClass().getMethod(getterName);
            Object value = method.invoke(source);
            return value != null ? String.valueOf(value).trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Long readLong(Object source, String getterName) {
        try {
            Method method = source.getClass().getMethod(getterName);
            Object value = method.invoke(source);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return value != null ? Long.parseLong(String.valueOf(value)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer readInteger(Object source, String getterName) {
        try {
            Method method = source.getClass().getMethod(getterName);
            Object value = method.invoke(source);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return value != null ? Integer.parseInt(String.valueOf(value)) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean readBoolean(Object source, String getterName) {
        try {
            Method method = source.getClass().getMethod(getterName);
            Object value = method.invoke(source);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return value != null ? Boolean.parseBoolean(String.valueOf(value)) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
