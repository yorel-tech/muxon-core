package com.scal.muxon.providers.libvirt;

import com.scal.muxon.providers.storage.StorageDiscoveryProvider;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Libvirt storage discovery implementation.
 * <p>
 * Discovers storage pools from Libvirt hypervisors and normalizes them
 * into the common capability model. This implementation uses the libvirt
 * Java bindings to connect to hypervisors and enumerate storage pools.
 * </p>
 */
public class LibvirtStorageDiscoveryProvider implements StorageDiscoveryProvider {

    private static final Logger log = LoggerFactory.getLogger(LibvirtStorageDiscoveryProvider.class);

    // Capability mappings for Libvirt storage types
    private static final Map<String, Map<String, Object>> STORAGE_TYPE_CAPABILITIES = Map.ofEntries(
        Map.entry("rbd", Map.of(
            "performance", "high",
            "media", "ssd",
            "redundancy", "replicated",
            "shared", true
        )),
        Map.entry("lvm", Map.of(
            "performance", "medium",
            "media", "ssd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("lvm-thin", Map.of(
            "performance", "medium",
            "media", "ssd",
            "redundancy", "none",
            "shared", false
        )),
        Map.entry("zfs", Map.of(
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
        ))
    );

    @Override
    public String getProviderType() {
        return "libvirt";
    }

    @Override
    public CompletableFuture<List<DiscoveredStorage>> discoverStorage(
            UUID providerId, 
            Map<String, Object> connectionInfo) {
        
        return CompletableFuture.supplyAsync(() -> {
            List<DiscoveredStorage> discoveredStorage = new ArrayList<>();

            log.info("[libvirt-storage] discoverStorage async task started: providerId={}", providerId);

            String connectionUri = (String) connectionInfo.get("uri");
            if (connectionUri == null || connectionUri.isEmpty()) {
                log.warn("[libvirt-storage] no connection URI in connectionInfo: providerId={}", providerId);
                return discoveredStorage;
            }

            Connect conn = null;
            try {
                log.info("[libvirt-storage] connecting (uri length={}): providerId={}",
                        connectionUri.length(), providerId);
                conn = new Connect(connectionUri, true);
                String[] poolNames = conn.listStoragePools();
                String[] inactivePoolNames = conn.listDefinedStoragePools();
                log.info("[libvirt-storage] connected; activePools={}, inactiveDefinedPools={}: providerId={}",
                        poolNames.length, inactivePoolNames.length, providerId);

                List<String> allPoolNames = new ArrayList<>();
                allPoolNames.addAll(Arrays.asList(poolNames));
                allPoolNames.addAll(Arrays.asList(inactivePoolNames));

                for (String poolName : allPoolNames) {
                    try {
                        StoragePool pool = conn.storagePoolLookupByName(poolName);
                        DiscoveredStorage storage = discoverPool(pool, providerId);
                        if (storage != null) {
                            discoveredStorage.add(storage);
                        }
                    } catch (LibvirtException e) {
                        log.warn("Failed to discover storage pool {}: {}", poolName, e.getMessage());
                    }
                }
                
                log.info("[libvirt-storage] discovered {} storage pool(s): providerId={}",
                    discoveredStorage.size(), providerId);

            } catch (LibvirtException e) {
                log.error("[libvirt-storage] libvirt error for provider {}: {}", providerId, e.getMessage(), e);
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (LibvirtException e) {
                        log.warn("Failed to close Libvirt connection: {}", e.getMessage());
                    }
                }
            }
            
            return discoveredStorage;
        });
    }

    @Override
    public CompletableFuture<StorageDiscoveryTestResult> testConnection(Map<String, Object> connectionInfo) {
        return CompletableFuture.supplyAsync(() -> {
            String connectionUri = (String) connectionInfo.get("uri");
            if (connectionUri == null || connectionUri.isEmpty()) {
                return StorageDiscoveryTestResult.failure("No connection URI provided");
            }

            Connect conn = null;
            try {
                conn = new Connect(connectionUri, true);
                String hostname = conn.getHostName();
                long version = conn.getVersion();
                
                Map<String, Object> info = new HashMap<>();
                info.put("hostname", hostname);
                info.put("libvirt_version", version);
                info.put("hypervisor", conn.getType());
                
                // Count storage pools
                String[] pools = conn.listStoragePools();
                String[] inactivePools = conn.listDefinedStoragePools();
                info.put("active_pools", pools.length);
                info.put("inactive_pools", inactivePools.length);
                
                return StorageDiscoveryTestResult.success(
                    "Successfully connected to " + hostname, 
                    info
                );
            } catch (LibvirtException e) {
                return StorageDiscoveryTestResult.failure(
                    "Failed to connect: " + e.getMessage()
                );
            } finally {
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (LibvirtException e) {
                        log.warn("Failed to close Libvirt connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public StorageDiscoveryCapabilities getCapabilities() {
        return new StorageDiscoveryCapabilities(
            List.of("rbd", "lvm", "lvm-thin", "zfs", "dir", "nfs", "netfs"),
            true,  // supportsAutoDiscovery
            true,  // supportsMetrics
            true,  // supportsMultiNode
            Map.of("uri", "Libvirt connection URI (e.g., qemu+ssh://host/system)")
        );
    }

    private DiscoveredStorage discoverPool(StoragePool pool, UUID providerId) {
        try {
            String poolName = pool.getName();
            String poolType = getPoolType(pool);
            StoragePoolInfo info = pool.getInfo();
            
            // Get capabilities for this storage type
            Map<String, Object> capabilities = new HashMap<>(
                STORAGE_TYPE_CAPABILITIES.getOrDefault(poolType, Map.of(
                    "performance", "medium",
                    "media", "unknown",
                    "redundancy", "none",
                    "shared", false
                ))
            );

            String hostPath = readPoolTargetPath(pool);
            if (hostPath != null && !hostPath.isBlank()) {
                capabilities.put("host_path", hostPath);
            }

            // Extract metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("total_gb", bytesToGb(info.capacity));
            metrics.put("free_gb", bytesToGb(info.available));
            metrics.put("used_gb", bytesToGb(info.allocation));
            metrics.put("state", info.state.toString());
            
            // Estimate IOPS based on storage type
            metrics.put("estimated_iops", estimateIops(poolType));
            
            log.debug("Discovered Libvirt pool: name={}, type={}, capabilities={}, metrics={}", 
                poolName, poolType, capabilities, metrics);
            
            return new DiscoveredStorage(
                poolName,
                poolName,
                poolType,
                capabilities,
                metrics,
                null // Libvirt pools are typically host-level
            );
            
        } catch (LibvirtException e) {
            log.warn("Failed to get info for storage pool: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Absolute directory for dir/netfs pools (used by core-services to replicate content library files).
     */
    private static String readPoolTargetPath(StoragePool pool) {
        try {
            return extractTargetPathFromPoolXml(pool.getXMLDesc(0));
        } catch (LibvirtException e) {
            return null;
        }
    }

    static String extractTargetPathFromPoolXml(String xml) {
        if (xml == null) {
            return null;
        }
        int target = xml.indexOf("<target");
        if (target < 0) {
            return null;
        }
        int pathStart = xml.indexOf("<path>", target);
        if (pathStart < 0) {
            return null;
        }
        int end = xml.indexOf("</path>", pathStart);
        if (end < 0) {
            return null;
        }
        return xml.substring(pathStart + 6, end).trim();
    }

    private String getPoolType(StoragePool pool) {
        try {
            String xml = pool.getXMLDesc(0);
            // Parse XML to get pool type
            if (xml.contains("type='rbd'") || xml.contains("type=\"rbd\"")) {
                return "rbd";
            } else if (xml.contains("type='lvm'") || xml.contains("type=\"lvm\"")) {
                return "lvm";
            } else if (xml.contains("type='zfs'") || xml.contains("type=\"zfs\"")) {
                return "zfs";
            } else if (xml.contains("type='dir'") || xml.contains("type=\"dir\"")) {
                return "dir";
            } else if (xml.contains("type='netfs'") || xml.contains("type=\"netfs\"")) {
                return "nfs";
            } else if (xml.contains("type='logical'") || xml.contains("type=\"logical\"")) {
                return "lvm-thin";
            }
            return "unknown";
        } catch (LibvirtException e) {
            log.warn("Failed to get pool XML: {}", e.getMessage());
            return "unknown";
        }
    }

    private long bytesToGb(long bytes) {
        return bytes / (1024L * 1024L * 1024L);
    }

    private int estimateIops(String poolType) {
        // Rough IOPS estimates based on storage type
        return switch (poolType) {
            case "rbd" -> 50000;
            case "nvme" -> 100000;
            case "lvm-thin", "zfs" -> 10000;
            case "lvm" -> 5000;
            case "dir", "nfs" -> 1000;
            default -> 3000;
        };
    }
}
