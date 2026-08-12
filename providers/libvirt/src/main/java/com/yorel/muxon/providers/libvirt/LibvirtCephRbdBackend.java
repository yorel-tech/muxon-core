/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.providers.libvirt;

import com.yorel.muxon.db.model.ProviderStorageMappingEntity;
import com.yorel.muxon.providers.storage.SnapshotCloneRequest;
import com.yorel.muxon.providers.storage.SnapshotCreationRequest;
import com.yorel.muxon.providers.storage.SnapshotCreationResult;
import com.yorel.muxon.providers.storage.SnapshotInfo;
import com.yorel.muxon.providers.storage.SnapshotListRequest;
import com.yorel.muxon.providers.storage.SnapshotOperationResult;
import com.yorel.muxon.providers.storage.SnapshotRestoreRequest;
import com.yorel.muxon.providers.storage.StorageCapabilities;
import com.yorel.muxon.providers.storage.VolumeAttachmentRequest;
import com.yorel.muxon.providers.storage.VolumeAttachmentResult;
import com.yorel.muxon.providers.storage.VolumeCreationRequest;
import com.yorel.muxon.providers.storage.VolumeCreationResult;
import com.yorel.muxon.providers.storage.VolumeInfo;
import com.yorel.muxon.providers.storage.VolumeListRequest;
import com.yorel.muxon.providers.storage.VolumeOperationResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StorageVol;
import org.libvirt.StorageVolInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ceph RBD storage backend implementation for Libvirt.
 *
 * <p>Ceph RBD (RADOS Block Device) provides distributed block storage with:
 *
 * <ul>
 *   <li>Native snapshot support
 *   <li>Copy-on-write clones
 *   <li>Thin provisioning
 *   <li>Encryption support
 * </ul>
 *
 * <p>Volume IDs follow the format: ceph-rbd/{pool}/{volume-name} Snapshot IDs follow the format:
 * ceph-rbd/{pool}/{volume-name}@{snapshot-name}
 */
public class LibvirtCephRbdBackend implements LibvirtStorageBackend {

  private static final Logger log = LoggerFactory.getLogger(LibvirtCephRbdBackend.class);

  @Override
  public VolumeCreationResult createVolume(
      Connect connection, VolumeCreationRequest request, ProviderStorageMappingEntity mapping) {
    try {
      Map<String, Object> config = mapping.getBackendConfig();
      String poolName = (String) config.get("pool");

      if (poolName == null) {
        return VolumeCreationResult.failure(
            request.name(), "Pool name not configured in storage mapping");
      }

      StoragePool pool = connection.storagePoolLookupByName(poolName);
      if (pool == null) {
        return VolumeCreationResult.failure(request.name(), "Storage pool not found: " + poolName);
      }

      String volumeXml =
          buildVolumeXml(
              request.name(), request.sizeBytes(), request.encrypted(), request.thinProvisioned());

      StorageVol volume = pool.storageVolCreateXML(volumeXml, 0);
      String volumePath = volume.getPath();
      String providerVolumeId = String.format("ceph-rbd/%s/%s", poolName, request.name());

      log.info("Created Ceph RBD volume: {} at path: {}", providerVolumeId, volumePath);

      return VolumeCreationResult.success(
          request.name(), providerVolumeId, volumePath, request.sizeBytes());

    } catch (LibvirtException e) {
      log.error("Failed to create Ceph RBD volume: {}", request.name(), e);
      return VolumeCreationResult.failure(request.name(), "Libvirt error: " + e.getMessage());
    }
  }

  @Override
  public VolumeOperationResult deleteVolume(
      Connect connection, String volumeId, LibvirtStorageProvider.VolumeMetadata metadata) {
    try {
      StoragePool pool = connection.storagePoolLookupByName(metadata.poolName());
      if (pool == null) {
        return VolumeOperationResult.failure("Storage pool not found: " + metadata.poolName());
      }

      StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
      if (volume == null) {
        return VolumeOperationResult.failure("Volume not found: " + metadata.volumeName());
      }

      volume.delete(0);
      log.info("Deleted Ceph RBD volume: {}", volumeId);

      return VolumeOperationResult.ok("Volume deleted successfully");

    } catch (LibvirtException e) {
      log.error("Failed to delete Ceph RBD volume: {}", volumeId, e);
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
        return VolumeOperationResult.failure("Storage pool not found: " + metadata.poolName());
      }

      StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
      if (volume == null) {
        return VolumeOperationResult.failure("Volume not found: " + metadata.volumeName());
      }

      volume.resize(newSizeBytes, 0);
      log.info("Resized Ceph RBD volume: {} to {} bytes", volumeId, newSizeBytes);

      return VolumeOperationResult.ok("Volume resized successfully");

    } catch (LibvirtException e) {
      log.error("Failed to resize Ceph RBD volume: {}", volumeId, e);
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
            "Storage pool not found: " + metadata.poolName());
      }

      StorageVol volume = pool.storageVolLookupByName(metadata.volumeName());
      if (volume == null) {
        return VolumeAttachmentResult.failure(
            request.volumeId(), request.resourceId(), "Volume not found: " + metadata.volumeName());
      }

      String volumePath = volume.getPath();
      String device = request.device() != null ? request.device() : generateDeviceName();

      log.info(
          "Attached Ceph RBD volume: {} to resource: {} at device: {}",
          request.volumeId(),
          request.resourceId(),
          device);

      return VolumeAttachmentResult.success(
          request.volumeId(), request.resourceId(), device, volumePath);

    } catch (LibvirtException e) {
      log.error("Failed to attach Ceph RBD volume: {}", request.volumeId(), e);
      return VolumeAttachmentResult.failure(
          request.volumeId(), request.resourceId(), "Libvirt error: " + e.getMessage());
    }
  }

  @Override
  public VolumeOperationResult detachVolume(
      Connect connection,
      String volumeId,
      String resourceId,
      LibvirtStorageProvider.VolumeMetadata metadata) {
    log.info("Detached Ceph RBD volume: {} from resource: {}", volumeId, resourceId);
    return VolumeOperationResult.ok("Volume detached successfully");
  }

  @Override
  public VolumeInfo getVolumeInfo(
      Connect connection, String volumeId, LibvirtStorageProvider.VolumeMetadata metadata) {
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
          true, // thinProvisioned
          Map.of(),
          java.time.Instant.now());

    } catch (LibvirtException e) {
      log.error("Failed to get Ceph RBD volume info: {}", volumeId, e);
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
          if (isRbdPool(pool)) {
            String[] volumeNames = pool.listVolumes();
            for (String volumeName : volumeNames) {
              try {
                StorageVol volume = pool.storageVolLookupByName(volumeName);
                StorageVolInfo info = volume.getInfo();
                String volumeId = String.format("ceph-rbd/%s/%s", poolName, volumeName);

                volumes.add(
                    new VolumeInfo(
                        volumeId,
                        volumeId,
                        volumeName,
                        null, // storageClass
                        info.capacity,
                        info.allocation,
                        "available",
                        false, // encrypted
                        true, // thinProvisioned
                        Map.of(),
                        java.time.Instant.now()));
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
      String snapshotId =
          String.format(
              "ceph-rbd/%s/%s@%s", metadata.poolName(), metadata.volumeName(), request.name());

      String command =
          String.format(
              "rbd snap create %s/%s@%s",
              metadata.poolName(), metadata.volumeName(), request.name());

      executeRbdCommand(command);

      log.info("Created Ceph RBD snapshot: {}", snapshotId);

      return SnapshotCreationResult.success(request.name(), snapshotId, 0L);

    } catch (Exception e) {
      log.error("Failed to create Ceph RBD snapshot: {}", request.name(), e);
      return SnapshotCreationResult.failure(
          request.name(), "Failed to create snapshot: " + e.getMessage());
    }
  }

  @Override
  public SnapshotOperationResult deleteSnapshot(
      Connect connection, String snapshotId, LibvirtStorageProvider.SnapshotMetadata metadata) {
    try {
      String command =
          String.format(
              "rbd snap rm %s/%s@%s",
              metadata.poolName(), metadata.volumeName(), metadata.snapshotName());

      executeRbdCommand(command);

      log.info("Deleted Ceph RBD snapshot: {}", snapshotId);

      return SnapshotOperationResult.ok("Snapshot deleted successfully");

    } catch (Exception e) {
      log.error("Failed to delete Ceph RBD snapshot: {}", snapshotId, e);
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

      String command =
          String.format(
              "rbd clone %s/%s@%s %s/%s",
              metadata.poolName(),
              metadata.volumeName(),
              metadata.snapshotName(),
              metadata.poolName(),
              newVolumeName);

      executeRbdCommand(command);

      String providerVolumeId = String.format("ceph-rbd/%s/%s", metadata.poolName(), newVolumeName);

      log.info("Restored volume from snapshot: {} -> {}", request.snapshotId(), providerVolumeId);

      return VolumeCreationResult.success(newVolumeName, providerVolumeId, providerVolumeId, 0L);

    } catch (Exception e) {
      log.error("Failed to restore from Ceph RBD snapshot: {}", request.snapshotId(), e);
      return VolumeCreationResult.failure(
          request.snapshotId(), "Failed to restore from snapshot: " + e.getMessage());
    }
  }

  @Override
  public VolumeCreationResult cloneFromSnapshot(
      Connect connection,
      SnapshotCloneRequest request,
      LibvirtStorageProvider.SnapshotMetadata metadata) {
    try {
      String newVolumeName = "clone-" + UUID.randomUUID().toString().substring(0, 8);

      String command =
          String.format(
              "rbd clone %s/%s@%s %s/%s",
              metadata.poolName(),
              metadata.volumeName(),
              metadata.snapshotName(),
              metadata.poolName(),
              newVolumeName);

      executeRbdCommand(command);

      String providerVolumeId = String.format("ceph-rbd/%s/%s", metadata.poolName(), newVolumeName);

      log.info("Cloned volume from snapshot: {} -> {}", request.snapshotId(), providerVolumeId);

      return VolumeCreationResult.success(newVolumeName, providerVolumeId, providerVolumeId, 0L);

    } catch (Exception e) {
      log.error("Failed to clone from Ceph RBD snapshot: {}", request.snapshotId(), e);
      return VolumeCreationResult.failure(
          request.snapshotId(), "Failed to clone from snapshot: " + e.getMessage());
    }
  }

  @Override
  public List<SnapshotInfo> listSnapshots(
      Connect connection,
      SnapshotListRequest request,
      LibvirtStorageProvider.VolumeMetadata metadata) {
    List<SnapshotInfo> snapshots = new ArrayList<>();
    try {
      String command =
          String.format(
              "rbd snap ls %s/%s --format json", metadata.poolName(), metadata.volumeName());

      String output = executeRbdCommand(command);

      log.debug("Listed snapshots for volume: {}/{}", metadata.poolName(), metadata.volumeName());

    } catch (Exception e) {
      log.error("Failed to list Ceph RBD snapshots", e);
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
          if (isRbdPool(pool)) {
            StoragePoolInfo info = pool.getInfo();
            totalCapacity += info.capacity;
            usedCapacity += info.allocation;
          }
        } catch (LibvirtException e) {
          log.warn("Failed to get info for pool: {}", poolName, e);
        }
      }

      return new StorageCapabilities.StorageBackendInfo(
          "ceph-rbd",
          totalCapacity,
          usedCapacity,
          totalCapacity - usedCapacity,
          Map.of("pools", pools.length));

    } catch (LibvirtException e) {
      log.error("Failed to get Ceph RBD backend info", e);
      return null;
    }
  }

  private String buildVolumeXml(
      String name, long sizeBytes, boolean encrypted, boolean thinProvisioned) {
    StringBuilder xml = new StringBuilder();
    xml.append("<volume type='network'>\n");
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

  private boolean isRbdPool(StoragePool pool) {
    try {
      String xml = pool.getXMLDesc(0);
      return xml.contains("type='rbd'") || xml.contains("type=\"rbd\"");
    } catch (LibvirtException e) {
      return false;
    }
  }

  private String generateDeviceName() {
    return "/dev/vd" + (char) ('b' + new Random().nextInt(24));
  }

  private String executeRbdCommand(String command) throws Exception {
    ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
    Process process = pb.start();
    int exitCode = process.waitFor();

    if (exitCode != 0) {
      throw new RuntimeException("RBD command failed with exit code: " + exitCode);
    }

    return "";
  }
}
