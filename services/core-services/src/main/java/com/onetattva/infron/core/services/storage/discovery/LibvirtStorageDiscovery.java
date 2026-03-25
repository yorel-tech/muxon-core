package com.onetattva.infron.core.services.storage.discovery;

import com.onetattva.infron.core.services.storage.CapabilityMappingService;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Libvirt storage discovery implementation.
 * <p>
 * Discovers storage pools from Libvirt hypervisors and normalizes them
 * into the common capability model.
 * </p>
 */
@Component
public class LibvirtStorageDiscovery implements StorageDiscoveryAdapter {

    private static final Logger log = LoggerFactory.getLogger(LibvirtStorageDiscovery.class);

    private final CapabilityMappingService capabilityMappingService;

    public LibvirtStorageDiscovery(CapabilityMappingService capabilityMappingService) {
        this.capabilityMappingService = capabilityMappingService;
    }

    @Override
    public String getProviderType() {
        return "libvirt";
    }

    @Override
    public List<DiscoveredStorage> discoverStorage(UUID providerId, Map<String, Object> connectionInfo) {
        List<DiscoveredStorage> discoveredStorage = new ArrayList<>();
        
        String connectionUri = (String) connectionInfo.get("uri");
        if (connectionUri == null || connectionUri.isEmpty()) {
            log.warn("No connection URI provided for Libvirt provider {}", providerId);
            return discoveredStorage;
        }

        Connect conn = null;
        try {
            conn = new Connect(connectionUri, true);
            String[] poolNames = conn.listStoragePools();
            String[] inactivePoolNames = conn.listDefinedStoragePools();
            
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
            
            log.info("Discovered {} storage pools from Libvirt provider {}", 
                discoveredStorage.size(), providerId);
            
        } catch (LibvirtException e) {
            log.error("Failed to connect to Libvirt provider {}: {}", providerId, e.getMessage(), e);
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
    }

    private DiscoveredStorage discoverPool(StoragePool pool, UUID providerId) {
        try {
            String poolName = pool.getName();
            String poolType = getPoolType(pool);
            StoragePoolInfo info = pool.getInfo();
            
            // Normalize capabilities based on pool type
            Map<String, Object> capabilities = capabilityMappingService.normalizeStorageType(
                poolType, getProviderType());
            
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

    private String getPoolType(StoragePool pool) {
        try {
            String xml = pool.getXMLDesc(0);
            // Parse XML to get pool type
            // For simplicity, we'll extract from the type attribute
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
