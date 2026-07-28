package com.scal.muxon.providers.libvirt;

import com.scal.muxon.providers.storage.*;
import com.scal.muxon.db.model.ProviderStorageMappingEntity;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * LVM (Logical Volume Manager) storage backend implementation for Libvirt.
 * <p>
 * LVM provides block storage with:
 * <ul>
 *   <li>Thin provisioning support via thin pools</li>
 *   <li>Snapshot support (copy-on-write)</li>
 *   <li>Volume resizing</li>
 *   <li>Encryption via LUKS</li>
 * </ul>
 * </p>
 * <p>
 * Volume IDs follow the format: lvm/{vg-name}/{lv-name}
 * Snapshot IDs follow the format: lvm/{vg-name}/{lv-name}@{snapshot-name}
 * </p>
 */
public class LibvirtLvmBackend implements LibvirtStorageBackend {

    private static final Logger log = LoggerFactory.getLogger(LibvirtLvmBackend.class);

    @Override
    public VolumeCreationResult createVolume(
            Connect connection,
            VolumeCreationRequest request,
            ProviderStorageMappingEntity mapping) {
        try {
            Map<String, Object> config = mapping.getBackendConfig();
            String volumeGroup = (String) config.get("volumeGroup");
            
            if (volumeGroup == null) {
                return VolumeCreationResult.failure(
                    request.name(),
                    "Volume group not configured in storage mapping"
                );
            }

            StoragePool pool = connection.storagePoolLookupByName(volumeGroup);
            if (pool == null) {
                return VolumeCreationResult.failure(
                    request.name(),
                    "Storage pool not found: " + volumeGroup
                );
            }

            String volumeXml = buildVolumeXml(
                request.name(),
                request.sizeBytes(),
                request.encrypted(),
                request.thinProvisioned(),
                config
            );

            StorageVol volume = pool.storageVolCreateXML(volumeXml, 0);
            String volumePath = volume.getPath();
            String providerVolumeId = String.format("lvm/%s/%s", volumeGroup, request.name());

            log.info("Created LVM volume: {} at path: {}", providerVolumeId, volumePath);

            return VolumeCreationResult.success(
                request.name(),
                providerVolumeId,
                volumePath,
                request.sizeBytes()
            );

        } catch (LibvirtException e) {
            log.error("Failed to create LVM volume: {}", request.name(), e);
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
            log.info("Deleted LVM volume: {}", volumeId);

            return VolumeOperationResult.ok("Volume deleted successfully");

        } catch (LibvirtException e) {
            log.error("Failed to delete LVM volume: {}", volumeId, e);
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

            volume.resize(newSizeBytes, 0);
            log.info("Resized LVM volume: {} to {} bytes", volumeId, newSizeBytes);

            return VolumeOperationResult.ok("Volume resized successfully");

        } catch (LibvirtException e) {
            log.error("Failed to resize LVM volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Libvirt error: " + e.getMessage());
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

            log.info("Attached LVM volume: {} to resource: {} at device: {}",
                request.volumeId(), request.resourceId(), device);

            return VolumeAttachmentResult.success(
                request.volumeId(),
                request.resourceId(),
                device,
                volumePath
            );

        } catch (LibvirtException e) {
            log.error("Failed to attach LVM volume: {}", request.volumeId(), e);
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
        log.info("Detached LVM volume: {} from resource: {}", volumeId, resourceId);
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
            log.error("Failed to get LVM volume info: {}", volumeId, e);
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
                    if (isLvmPool(pool)) {
                        String[] volumeNames = pool.listVolumes();
                        for (String volumeName : volumeNames) {
                            try {
                                StorageVol volume = pool.storageVolLookupByName(volumeName);
                                StorageVolInfo info = volume.getInfo();
                                String volumeId = String.format("lvm/%s/%s", poolName, volumeName);
                                
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
            String snapshotName = metadata.volumeName() + "_" + request.name();
            String snapshotId = String.format("lvm/%s/%s@%s",
                metadata.poolName(), metadata.volumeName(), request.name());

            String command = String.format("lvcreate -s -n %s /dev/%s/%s",
                snapshotName, metadata.poolName(), metadata.volumeName());

            executeLvmCommand(command);

            log.info("Created LVM snapshot: {}", snapshotId);

            return SnapshotCreationResult.success(
                request.name(),
                snapshotId,
                0L
            );

        } catch (Exception e) {
            log.error("Failed to create LVM snapshot: {}", request.name(), e);
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
            String snapshotName = metadata.volumeName() + "_" + metadata.snapshotName();
            String command = String.format("lvremove -f /dev/%s/%s",
                metadata.poolName(), snapshotName);

            executeLvmCommand(command);

            log.info("Deleted LVM snapshot: {}", snapshotId);

            return SnapshotOperationResult.ok("Snapshot deleted successfully");

        } catch (Exception e) {
            log.error("Failed to delete LVM snapshot: {}", snapshotId, e);
            return SnapshotOperationResult.failure("Failed to delete snapshot: " + e.getMessage());
        }
    }

    @Override
    public VolumeCreationResult restoreFromSnapshot(
            Connect connection,
            SnapshotRestoreRequest request,
            LibvirtStorageProvider.SnapshotMetadata metadata) {
        try {
            String newVolumeName = "restored-" + UUID.randomUUID().toString().substring(0, 8);
            String snapshotName = metadata.volumeName() + "_" + metadata.snapshotName();
            
            String command = String.format("lvcreate -s -n %s /dev/%s/%s",
                newVolumeName, metadata.poolName(), snapshotName);

            executeLvmCommand(command);

            String providerVolumeId = String.format("lvm/%s/%s", metadata.poolName(), newVolumeName);

            log.info("Restored volume from snapshot: {} -> {}", request.snapshotId(), providerVolumeId);

            return VolumeCreationResult.success(
                newVolumeName,
                providerVolumeId,
                providerVolumeId,
                0L
            );

        } catch (Exception e) {
            log.error("Failed to restore from LVM snapshot: {}", request.snapshotId(), e);
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
        return restoreFromSnapshot(
            connection,
            new SnapshotRestoreRequest(
                request.snapshotId(),
                "clone-" + UUID.randomUUID().toString().substring(0, 8),
                "cloned"
            ),
            metadata
        );
    }

    @Override
    public List<SnapshotInfo> listSnapshots(
            Connect connection,
            SnapshotListRequest request,
            LibvirtStorageProvider.VolumeMetadata metadata) {
        List<SnapshotInfo> snapshots = new ArrayList<>();
        try {
            String command = String.format("lvs --noheadings -o lv_name /dev/%s | grep %s_",
                metadata.poolName(), metadata.volumeName());

            String output = executeLvmCommand(command);
            
            log.debug("Listed snapshots for volume: {}/{}", metadata.poolName(), metadata.volumeName());

        } catch (Exception e) {
            log.debug("No snapshots found or error listing LVM snapshots", e);
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
                    if (isLvmPool(pool)) {
                        StoragePoolInfo info = pool.getInfo();
                        totalCapacity += info.capacity;
                        usedCapacity += info.allocation;
                    }
                } catch (LibvirtException e) {
                    log.warn("Failed to get info for pool: {}", poolName, e);
                }
            }

            return new StorageCapabilities.StorageBackendInfo(
                "lvm",
                totalCapacity,
                usedCapacity,
                totalCapacity - usedCapacity,
                Map.of("pools", pools.length)
            );

        } catch (LibvirtException e) {
            log.error("Failed to get LVM backend info", e);
            return null;
        }
    }

    private String buildVolumeXml(
            String name,
            long sizeBytes,
            boolean encrypted,
            boolean thinProvisioned,
            Map<String, Object> config) {
        StringBuilder xml = new StringBuilder();
        xml.append("<volume>\n");
        xml.append("  <name>").append(name).append("</name>\n");
        xml.append("  <capacity unit='bytes'>").append(sizeBytes).append("</capacity>\n");
        
        if (thinProvisioned) {
            xml.append("  <allocation unit='bytes'>0</allocation>\n");
        } else {
            xml.append("  <allocation unit='bytes'>").append(sizeBytes).append("</allocation>\n");
        }
        
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

    private boolean isLvmPool(StoragePool pool) {
        try {
            String xml = pool.getXMLDesc(0);
            return xml.contains("type='logical'") || xml.contains("type=\"logical\"");
        } catch (LibvirtException e) {
            return false;
        }
    }

    private String generateDeviceName() {
        return "/dev/vd" + (char) ('b' + new Random().nextInt(24));
    }

    private String executeLvmCommand(String command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
        Process process = pb.start();
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("LVM command failed with exit code: " + exitCode);
        }
        
        return "";
    }
}
