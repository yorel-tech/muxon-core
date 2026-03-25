package com.onetattva.infron.core.services.storage.discovery;

import com.onetattva.infron.core.services.storage.CapabilityMappingService;
import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;

/**
 * Proxmox storage discovery implementation.
 * <p>
 * Discovers storage configurations from Proxmox clusters and normalizes them
 * into the common capability model.
 * </p>
 */
@Component
public class ProxmoxStorageDiscovery implements StorageDiscoveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(ProxmoxStorageDiscovery.class);

    private final CapabilityMappingService capabilityMappingService;

    public ProxmoxStorageDiscovery(CapabilityMappingService capabilityMappingService) {
        this.capabilityMappingService = capabilityMappingService;
    }

    @Override
    public String getProviderType() {
        return "proxmox";
    }

    @Override
    public List<DiscoveredStorage> discoverStorage(UUID providerId, Map<String, Object> connectionInfo) {
        List<DiscoveredStorage> discoveredStorage = new ArrayList<>();
        
        try {
            Proxmox proxmox = createProxmoxClient(connectionInfo);
            
            // Get all nodes
            Object nodesApi = invokeMethod(proxmox, "getNodes");
            Object nodesIndexApi = invokeMethod(nodesApi, "getIndex");
            Object nodesResult = invokeMethod(nodesIndexApi, "execute");
            
            if (!(nodesResult instanceof List<?> nodes)) {
                log.warn("Failed to get nodes from Proxmox provider {}", providerId);
                return discoveredStorage;
            }
            
            Set<String> processedStorage = new HashSet<>();
            
            for (Object nodeObj : nodes) {
                String nodeName = readString(nodeObj, "getNode");
                if (nodeName == null || nodeName.isEmpty()) {
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
                        for (Object storageObj : storageList) {
                            DiscoveredStorage storage = discoverStorageEntry(
                                storageObj, nodeName, providerId, processedStorage);
                            if (storage != null) {
                                discoveredStorage.add(storage);
                            }
                        }
                    }
                }
            }
            
            log.info("Discovered {} storage entries from Proxmox provider {}", 
                discoveredStorage.size(), providerId);
            
        } catch (Exception e) {
            log.error("Failed to discover storage from Proxmox provider {}: {}", 
                providerId, e.getMessage(), e);
        }
        
        return discoveredStorage;
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
            
            // Normalize capabilities based on storage type
            Map<String, Object> capabilities = capabilityMappingService.normalizeStorageType(
                storageType, getProviderType());
            
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
            
            // Get content types
            String content = readString(storageObj, "getContent");
            if (content != null) {
                metrics.put("content_types", Arrays.asList(content.split(",")));
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
        
        if (username == null || password == null) {
            throw new IllegalArgumentException("Missing username or password");
        }
        
        // Handle username@realm format
        int atIndex = username.indexOf('@');
        if (atIndex != -1) {
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
