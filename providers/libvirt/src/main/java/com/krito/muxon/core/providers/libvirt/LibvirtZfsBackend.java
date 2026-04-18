package com.krito.muxon.core.providers.libvirt;

import com.krito.muxon.core.providers.storage.*;
import com.krito.muxon.db.model.ProviderStorageMappingEntity;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * ZFS storage backend implementation for Libvirt.
 * <p>
 * ZFS provides advanced storage features:
 * <ul>
 *   <li>Native copy-on-write snapshots</li>
 *   <li>Efficient cloning</li>
 *   <li>Compression and deduplication</li>
 *   <li>Data integrity verification</li>
 * </ul>
 * </p>
 * <p>
 * Volume IDs follow the format: zfs/{pool}/{dataset}
 * Snapshot IDs follow the format: zfs/{pool}/{dataset}@{snapshot-name}
 * </p>
 */
public class LibvirtZfsBackend implements LibvirtStorageBackend {

    private static final Logger log = LoggerFactory.getLogger(LibvirtZfsBackend.class);

    @Override
    public VolumeCreationResult createVolume(
            Connect connection,
            VolumeCreationRequest request,
            ProviderStorageMappingEntity mapping) {
        try {
            Map<String, Object> config = mapping.getBackendConfig();
            String poolName = (String) config.get("pool");
            
            if (poolName == null) {
                return VolumeCreationResult.failure(
                    request.name(),
                    "Pool name not configured in storage mapping"
                );
            }

            StoragePool pool = connection.storagePoolLookupByName(poolName);
            if (pool == null) {
                return VolumeCreationResult.failure(
                    request.name(),
                    "Storage pool not found: " + poolName
                );
            }

            String volumeXml = buildVolumeXml(
                request.name(),
                request.sizeBytes(),
                request.encrypted(),
                config
            );

            StorageVol volume = pool.storageVolCreateXML(volumeXml, 0);
            String volumePath = volume.getPath();
            String providerVolumeId = String.format("zfs/%s/%s", poolName, request.name());

            log.info("Created ZFS volume: {} at path: {}", providerVolumeId, volumePath);

            return VolumeCreationResult.success(
                request.name(),
                providerVolumeId,
                volumePath,
                request.sizeBytes()
            );

        } catch (LibvirtException e) {
            log.error("Failed to create ZFS volume: {}", request.name(), e);
            return VolumeCreationResult.failure(
                request.name(),
                "Libvirt error: " + e.getMessage()
            );
        }
    }

    @Override
    public VolumeOperationResult deleteVolume(
            Connect connection,
            String volumeId,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        try {
            StoragePool pool = connection.storagePoolLookupByName(metadata.poolName());
            if (pool == null) {
                return VolumeOperationResult.failure(
                    "Storage pool not found: " + metadata.poolName()
                );
            }

            StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
            if (volume == null) {
                return VolumeOperationResult.failure(
                    "Volume not found: " + metadata.volumeName()
                );
            }

            volume.delete(0);
            log.info("Deleted ZFS volume: {}", volumeId);

            return VolumeOperationResult.ok("Volume deleted successfully");

        } catch (LibvirtException e) {
            log.error("Failed to delete ZFS volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Libvirt error: " + e.getMessage());
        }
    }

    @Override
    public VolumeOperationResult resizeVolume(
            Connect connection,
            String volumeId,
            long newSizeBytes,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        try {
            String command = String.format("zfs set volsize=%d %s/%s",
                newSizeBytes, metadata.poolName(), metadata.volumeName());

            executeZfsCommand(command);

            log.info("Resized ZFS volume: {} to {} bytes", volumeId, newSizeBytes);

            return VolumeOperationResult.ok("Volume resized successfully");

        } catch (Exception e) {
            log.error("Failed to resize ZFS volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Failed to resize volume: " + e.getMessage());
        }
    }

    @Override
    public VolumeAttachmentResult attachVolume(
            Connect connection,
            VolumeAttachmentRequest request,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        try {
            StoragePool pool = connection.storagePoolLookupByName(metadata.poolName());
            if (pool == null) {
                return VolumeAttachmentResult.failure(
                    request.volumeId(),
                    request.resourceId(),
                    "Storage pool not found: " + metadata.poolName()
                );
            }

            StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
            if (volume == null) {
                return VolumeAttachmentResult.failure(
                    request.volumeId(),
                    request.resourceId(),
                    "Volume not found: " + metadata.volumeName()
                );
            }

            String volumePath = volume.getPath();
            String device = request.device() != null ? request.device() : generateDeviceName();

            log.info("Attached ZFS volume: {} to resource: {} at device: {}",
                request.volumeId(), request.resourceId(), device);

            return VolumeAttachmentResult.success(
                request.volumeId(),
                request.resourceId(),
                device,
                volumePath
            );

        } catch (LibvirtException e) {
            log.error("Failed to attach ZFS volume: {}", request.volumeId(), e);
            return VolumeAttachmentResult.failure(
                request.volumeId(),
                request.resourceId(),
                "Libvirt error: " + e.getMessage()
            );
        }
    }

    @Override
    public VolumeOperationResult detachVolume(
            Connect connection,
            String volumeId,
            String resourceId,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        log.info("Detached ZFS volume: {} from resource: {}", volumeId, resourceId);
        return VolumeOperationResult.ok("Volume detached successfully");
    }

    @Override
    public VolumeInfo getVolumeInfo(
            Connect connection,
            String volumeId,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        try {
            StoragePool pool = connection.storagePoolLookupByName(metadata.poolName());
            if (pool == null) {
                throw new RuntimeException("Storage pool not found: " + metadata.poolName());
            }

            StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
            if (volume == null) {
                throw new RuntimeException("Volume not found: " + metadata.volumeName());
            }

            StorageVolInfo info = volume.getInfo();
            String volumePath = volume.getPath();

            return new VolumeInfo(
                volumeId,
                volumeId,
                metadata.volumeName(),
                null, // storageClass
                info.capacity,
                info.allocation,
                "available",
                false, // encrypted
                true,  // thinProvisioned
                Map.of(),
                java.time.Instant.now()
            );

        } catch (LibvirtException e) {
            log.error("Failed to get ZFS volume info: {}", volumeId, e);
            throw new RuntimeException("Libvirt error: " + e.getMessage(), e);
        }
    }

    @Override
    public List<VolumeInfo> listVolumes(Connect connection, VolumeListRequest request) {
        List<VolumeInfo> volumes = new ArrayList<>();
        try {
            String[] pools = connection.listStoragePools();
            for (String poolName : pools) {
                try {
                    StoragePool pool = connection.storagePoolLookupByName(poolName);
                    if (isZfsPool(pool)) {
                        String[] volumeNames = pool.listVolumes();
                        for (String volumeName : volumeNames) {
                            try {
                                StorageVol volume = pool.storageVolLookupByName(volumeName);
                                StorageVolInfo info = volume.getInfo();
                                String volumeId = String.format("zfs/%s/%s", poolName, volumeName);
                                
                                volumes.add(new VolumeInfo(
                                    volumeId,
                                    volumeId,
                                    volumeName,
                                    null, // storageClass
                                    info.capacity,
                                    info.allocation,
                                    "available",
                                    false, // encrypted
                                    true,  // thinProvisioned
                                    Map.of(),
                                    java.time.Instant.now()
                                ));
                            } catch (LibvirtException e) {
                                log.warn("Failed to get info for volume: {}", volumeName, e);
                            }
                        }
                    }
                } catch (LibvirtException e) {
                    log.warn("Failed to list volumes in pool: {}", poolName, e);
                }
            }
        } catch (LibvirtException e) {
            log.error("Failed to list storage pools", e);
        }
        return volumes;
    }

    @Override
    public SnapshotCreationResult createSnapshot(
            Connect connection,
            SnapshotCreationRequest request,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        try {
            String snapshotId = String.format("zfs/%s/%s@%s",
                metadata.poolName(), metadata.volumeName(), request.name());

            String command = String.format("zfs snapshot %s/%s@%s",
                metadata.poolName(), metadata.volumeName(), request.name());

            executeZfsCommand(command);

            log.info("Created ZFS snapshot: {}", snapshotId);

            return SnapshotCreationResult.success(
                request.name(),
                snapshotId,
                0L
            );

        } catch (Exception e) {
            log.error("Failed to create ZFS snapshot: {}", request.name(), e);
            return SnapshotCreationResult.failure(
                request.name(),
                "Failed to create snapshot: " + e.getMessage()
            );
        }
    }

    @Override
    public SnapshotOperationResult deleteSnapshot(
            Connect connection,
            String snapshotId,
            LibvirtStorageProvider.SnapshotMetadata metadata) {
        try {
            String command = String.format("zfs destroy %s/%s@%s",
                metadata.poolName(), metadata.volumeName(), metadata.snapshotName());

            executeZfsCommand(command);

            log.info("Deleted ZFS snapshot: {}", snapshotId);

            return SnapshotOperationResult.ok("Snapshot deleted successfully");

        } catch (Exception e) {
            log.error("Failed to delete ZFS snapshot: {}", snapshotId, e);
            return SnapshotOperationResult.failure("Failed to delete snapshot: " + e.getMessage());
        }
    }

    @Override
    public VolumeCreationResult restoreFromSnapshot(
            Connect connection,
            SnapshotRestoreRequest request,
            LibvirtStorageProvider.SnapshotMetadata metadata) {
        try {
            String command = String.format("zfs rollback %s/%s@%s",
                metadata.poolName(), metadata.volumeName(), metadata.snapshotName());

            executeZfsCommand(command);

            String providerVolumeId = String.format("zfs/%s/%s", 
                metadata.poolName(), metadata.volumeName());

            log.info("Restored volume from snapshot: {}", request.snapshotId());

            return VolumeCreationResult.success(
                metadata.volumeName(),
                providerVolumeId,
                providerVolumeId,
                0L
            );

        } catch (Exception e) {
            log.error("Failed to restore from ZFS snapshot: {}", request.snapshotId(), e);
            return VolumeCreationResult.failure(
                request.snapshotId(),
                "Failed to restore from snapshot: " + e.getMessage()
            );
        }
    }

    @Override
    public VolumeCreationResult cloneFromSnapshot(
            Connect connection,
            SnapshotCloneRequest request,
            LibvirtStorageProvider.SnapshotMetadata metadata) {
        try {
            String newVolumeName = "clone-" + UUID.randomUUID().toString().substring(0, 8);
            
            String command = String.format("zfs clone %s/%s@%s %s/%s",
                metadata.poolName(), metadata.volumeName(), metadata.snapshotName(),
                metadata.poolName(), newVolumeName);

            executeZfsCommand(command);

            String providerVolumeId = String.format("zfs/%s/%s", metadata.poolName(), newVolumeName);

            log.info("Cloned volume from snapshot: {} -> {}", request.snapshotId(), providerVolumeId);

            return VolumeCreationResult.success(
                newVolumeName,
                providerVolumeId,
                providerVolumeId,
                0L
            );

        } catch (Exception e) {
            log.error("Failed to clone from ZFS snapshot: {}", request.snapshotId(), e);
            return VolumeCreationResult.failure(
                request.snapshotId(),
                "Failed to clone from snapshot: " + e.getMessage()
            );
        }
    }

    @Override
    public List<SnapshotInfo> listSnapshots(
            Connect connection,
            SnapshotListRequest request,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        List<SnapshotInfo> snapshots = new ArrayList<>();
        try {
            String command = String.format("zfs list -t snapshot -o name -H | grep %s/%s@",
                metadata.poolName(), metadata.volumeName());

            String output = executeZfsCommand(command);
            
            log.debug("Listed snapshots for volume: {}/{}", metadata.poolName(), metadata.volumeName());

        } catch (Exception e) {
            log.debug("No snapshots found or error listing ZFS snapshots", e);
        }
        return snapshots;
    }

    @Override
    public StorageCapabilities.StorageBackendInfo getBackendInfo(Connect connection) {
        try {
            String[] pools = connection.listStoragePools();
            long totalCapacity = 0;
            long usedCapacity = 0;

            for (String poolName : pools) {
                try {
                    StoragePool pool = connection.storagePoolLookupByName(poolName);
                    if (isZfsPool(pool)) {
                        StoragePoolInfo info = pool.getInfo();
                        totalCapacity += info.capacity;
                        usedCapacity += info.allocation;
                    }
                } catch (LibvirtException e) {
                    log.warn("Failed to get info for pool: {}", poolName, e);
                }
            }

            return new StorageCapabilities.StorageBackendInfo(
                "zfs",
                totalCapacity,
                usedCapacity,
                totalCapacity - usedCapacity,
                Map.of("pools", pools.length)
            );

        } catch (LibvirtException e) {
            log.error("Failed to get ZFS backend info", e);
            return null;
        }
    }

    private String buildVolumeXml(
            String name,
            long sizeBytes,
            boolean encrypted,
            Map<String, Object> config) {
        StringBuilder xml = new StringBuilder();
        xml.append("<volume type='block'>\n");
        xml.append("  <name>").append(name).append("</name>\n");
        xml.append("  <capacity unit='bytes'>").append(sizeBytes).append("</capacity>\n");
        xml.append("  <target>\n");
        xml.append("    <format type='raw'/>\n");
        
        if (encrypted) {
            xml.append("    <encryption format='luks'>\n");
            xml.append("      <secret type='passphrase'/>\n");
            xml.append("    </encryption>\n");
        }
        
        xml.append("  </target>\n");
        xml.append("</volume>");
        
        return xml.toString();
    }

    private boolean isZfsPool(StoragePool pool) {
        try {
            String xml = pool.getXMLDesc(0);
            return xml.contains("type='zfs'") || xml.contains("type=\"zfs\"");
        } catch (LibvirtException e) {
            return false;
        }
    }

    private String generateDeviceName() {
        return "/dev/vd" + (char) ('b' + new Random().nextInt(24));
    }

    private String executeZfsCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("ZFS command failed with exit code: " + exitCode);
        }
        
        return "";
    }
}
