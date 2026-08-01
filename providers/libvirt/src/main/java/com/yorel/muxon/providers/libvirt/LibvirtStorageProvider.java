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
import com.yorel.muxon.db.repository.ProviderStorageMappingRepository;
import com.yorel.muxon.providers.storage.BucketCreationRequest;
import com.yorel.muxon.providers.storage.BucketCreationResult;
import com.yorel.muxon.providers.storage.BucketInfo;
import com.yorel.muxon.providers.storage.BucketListRequest;
import com.yorel.muxon.providers.storage.BucketOperationResult;
import com.yorel.muxon.providers.storage.SnapshotCloneRequest;
import com.yorel.muxon.providers.storage.SnapshotCreationRequest;
import com.yorel.muxon.providers.storage.SnapshotCreationResult;
import com.yorel.muxon.providers.storage.SnapshotInfo;
import com.yorel.muxon.providers.storage.SnapshotListRequest;
import com.yorel.muxon.providers.storage.SnapshotOperationResult;
import com.yorel.muxon.providers.storage.SnapshotRestoreRequest;
import com.yorel.muxon.providers.storage.StorageCapabilities;
import com.yorel.muxon.providers.storage.StorageProvider;
import com.yorel.muxon.providers.storage.VolumeAttachmentRequest;
import com.yorel.muxon.providers.storage.VolumeAttachmentResult;
import com.yorel.muxon.providers.storage.VolumeCreationRequest;
import com.yorel.muxon.providers.storage.VolumeCreationResult;
import com.yorel.muxon.providers.storage.VolumeInfo;
import com.yorel.muxon.providers.storage.VolumeListRequest;
import com.yorel.muxon.providers.storage.VolumeOperationResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.libvirt.Connect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Libvirt storage provider implementation supporting multiple storage backends.
 *
 * <p>Supports the following storage backends:
 *
 * <ul>
 *   <li>Ceph RBD - Distributed block storage with snapshots and clones
 *   <li>LVM - Logical Volume Manager with thin provisioning
 *   <li>ZFS - Copy-on-write filesystem with snapshots
 * </ul>
 *
 * <p>This provider delegates storage operations to backend-specific handlers based on the storage
 * class mapping configuration. Each backend handler implements the specific libvirt storage pool
 * operations for that backend type.
 */
public class LibvirtStorageProvider implements StorageProvider {

  private static final Logger log = LoggerFactory.getLogger(LibvirtStorageProvider.class);

  private final UUID providerId;
  private final LibvirtMultiNodeConnectionManager connectionManager;
  private final ProviderStorageMappingRepository mappingRepository;
  private final LibvirtStorageBackendFactory backendFactory;

  public LibvirtStorageProvider(
      UUID providerId,
      LibvirtMultiNodeConnectionManager connectionManager,
      ProviderStorageMappingRepository mappingRepository) {
    this.providerId = providerId;
    this.connectionManager = connectionManager;
    this.mappingRepository = mappingRepository;
    this.backendFactory = new LibvirtStorageBackendFactory();
  }

  @Override
  public String id() {
    return "libvirt-storage-" + providerId;
  }

  @Override
  public String description() {
    return "Libvirt storage provider supporting Ceph RBD, LVM, and ZFS backends";
  }

  @Override
  public CompletableFuture<VolumeCreationResult> createVolume(VolumeCreationRequest request) {
    log.info(
        "Creating volume: name={}, storageClass={}, size={}",
        request.name(),
        request.storageClass(),
        request.sizeBytes());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            ProviderStorageMappingEntity mapping = getStorageMapping(request.storageClass());
            LibvirtStorageBackend backend = backendFactory.getBackend(mapping.getBackendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.createVolume(connection, request, mapping);
          } catch (Exception e) {
            log.error("Failed to create volume: {}", request.name(), e);
            return VolumeCreationResult.failure(
                request.name(), "Failed to create volume: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeOperationResult> deleteVolume(String volumeId) {
    log.info("Deleting volume: {}", volumeId);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(volumeId);
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.deleteVolume(connection, volumeId, metadata);
          } catch (Exception e) {
            log.error("Failed to delete volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Failed to delete volume: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeOperationResult> resizeVolume(String volumeId, long newSizeBytes) {
    log.info("Resizing volume: {} to {} bytes", volumeId, newSizeBytes);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(volumeId);
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.resizeVolume(connection, volumeId, newSizeBytes, metadata);
          } catch (Exception e) {
            log.error("Failed to resize volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Failed to resize volume: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeAttachmentResult> attachVolume(VolumeAttachmentRequest request) {
    log.info("Attaching volume: {} to resource: {}", request.volumeId(), request.resourceId());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(request.volumeId());
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.attachVolume(connection, request, metadata);
          } catch (Exception e) {
            log.error("Failed to attach volume: {}", request.volumeId(), e);
            return VolumeAttachmentResult.failure(
                request.volumeId(),
                request.resourceId(),
                "Failed to attach volume: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeOperationResult> detachVolume(String volumeId, String resourceId) {
    log.info("Detaching volume: {} from resource: {}", volumeId, resourceId);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(volumeId);
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.detachVolume(connection, volumeId, resourceId, metadata);
          } catch (Exception e) {
            log.error("Failed to detach volume: {}", volumeId, e);
            return VolumeOperationResult.failure("Failed to detach volume: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeInfo> getVolumeInfo(String volumeId) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(volumeId);
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.getVolumeInfo(connection, volumeId, metadata);
          } catch (Exception e) {
            log.error("Failed to get volume info: {}", volumeId, e);
            throw new RuntimeException("Failed to get volume info: " + e.getMessage(), e);
          }
        });
  }

  @Override
  public CompletableFuture<List<VolumeInfo>> listVolumes(VolumeListRequest request) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              return Collections.emptyList();
            }

            List<VolumeInfo> allVolumes = new ArrayList<>();
            for (String backendType : List.of("ceph-rbd", "lvm", "zfs")) {
              try {
                LibvirtStorageBackend backend = backendFactory.getBackend(backendType);
                List<VolumeInfo> volumes = backend.listVolumes(connection, request);
                allVolumes.addAll(volumes);
              } catch (Exception e) {
                log.warn("Failed to list volumes for backend {}: {}", backendType, e.getMessage());
              }
            }
            return allVolumes;
          } catch (Exception e) {
            log.error("Failed to list volumes", e);
            return Collections.emptyList();
          }
        });
  }

  @Override
  public CompletableFuture<SnapshotCreationResult> createSnapshot(SnapshotCreationRequest request) {
    log.info("Creating snapshot: {} for volume: {}", request.name(), request.volumeId());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(request.volumeId());
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.createSnapshot(connection, request, metadata);
          } catch (Exception e) {
            log.error("Failed to create snapshot: {}", request.name(), e);
            return SnapshotCreationResult.failure(
                request.name(), "Failed to create snapshot: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<SnapshotOperationResult> deleteSnapshot(String snapshotId) {
    log.info("Deleting snapshot: {}", snapshotId);

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            SnapshotMetadata metadata = parseSnapshotId(snapshotId);
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.deleteSnapshot(connection, snapshotId, metadata);
          } catch (Exception e) {
            log.error("Failed to delete snapshot: {}", snapshotId, e);
            return SnapshotOperationResult.failure("Failed to delete snapshot: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeCreationResult> restoreFromSnapshot(
      SnapshotRestoreRequest request) {
    log.info("Restoring volume from snapshot: {}", request.snapshotId());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            SnapshotMetadata metadata = parseSnapshotId(request.snapshotId());
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.restoreFromSnapshot(connection, request, metadata);
          } catch (Exception e) {
            log.error("Failed to restore from snapshot: {}", request.snapshotId(), e);
            return VolumeCreationResult.failure(
                request.snapshotId(), "Failed to restore from snapshot: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<VolumeCreationResult> cloneFromSnapshot(SnapshotCloneRequest request) {
    log.info("Cloning volume from snapshot: {}", request.snapshotId());

    return CompletableFuture.supplyAsync(
        () -> {
          try {
            SnapshotMetadata metadata = parseSnapshotId(request.snapshotId());
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              throw new IllegalStateException("No active libvirt connection available");
            }

            return backend.cloneFromSnapshot(connection, request, metadata);
          } catch (Exception e) {
            log.error("Failed to clone from snapshot: {}", request.snapshotId(), e);
            return VolumeCreationResult.failure(
                request.snapshotId(), "Failed to clone from snapshot: " + e.getMessage());
          }
        });
  }

  @Override
  public CompletableFuture<List<SnapshotInfo>> listSnapshots(SnapshotListRequest request) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            VolumeMetadata metadata = parseVolumeId(request.volumeId());
            LibvirtStorageBackend backend = backendFactory.getBackend(metadata.backendType());

            Connect connection = connectionManager.getAnyConnection();
            if (connection == null) {
              return Collections.emptyList();
            }

            return backend.listSnapshots(connection, request, metadata);
          } catch (Exception e) {
            log.error("Failed to list snapshots for volume: {}", request.volumeId(), e);
            return Collections.emptyList();
          }
        });
  }

  @Override
  public CompletableFuture<BucketCreationResult> createBucket(BucketCreationRequest request) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Object storage not supported by Libvirt provider"));
  }

  @Override
  public CompletableFuture<BucketOperationResult> deleteBucket(String bucketId) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Object storage not supported by Libvirt provider"));
  }

  @Override
  public CompletableFuture<BucketInfo> getBucketInfo(String bucketId) {
    return CompletableFuture.failedFuture(
        new UnsupportedOperationException("Object storage not supported by Libvirt provider"));
  }

  @Override
  public CompletableFuture<List<BucketInfo>> listBuckets(BucketListRequest request) {
    return CompletableFuture.supplyAsync(Collections::emptyList);
  }

  @Override
  public CompletableFuture<StorageCapabilities> getCapabilities() {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            List<String> supportedClasses =
                mappingRepository.findByProviderIdAndEnabled(providerId, true).stream()
                    .map(ProviderStorageMappingEntity::getStorageClass)
                    .distinct()
                    .toList();

            Map<String, StorageCapabilities.StorageBackendInfo> backends = new HashMap<>();

            Connect connection = connectionManager.getAnyConnection();
            if (connection != null) {
              for (String backendType : List.of("ceph-rbd", "lvm", "zfs")) {
                try {
                  LibvirtStorageBackend backend = backendFactory.getBackend(backendType);
                  StorageCapabilities.StorageBackendInfo info = backend.getBackendInfo(connection);
                  if (info != null) {
                    backends.put(backendType, info);
                  }
                } catch (Exception e) {
                  log.debug("Backend {} not available: {}", backendType, e.getMessage());
                }
              }
            }

            return new StorageCapabilities(
                supportedClasses,
                backends,
                new StorageCapabilities.StorageFeatures(
                    true, // thinProvisioning
                    true, // encryption
                    true, // snapshots
                    true, // clones
                    false, // replication (not implemented in OSS)
                    false, // qos (not implemented in OSS)
                    false // objectStorage
                    ),
                new StorageCapabilities.StorageLimits(
                    16L * 1024 * 1024 * 1024 * 1024, // 16TB max volume
                    1000, // max volumes per workspace
                    100, // max snapshots per volume
                    0 // no bucket support
                    ));
          } catch (Exception e) {
            log.error("Failed to get storage capabilities", e);
            throw new RuntimeException("Failed to get storage capabilities", e);
          }
        });
  }

  private ProviderStorageMappingEntity getStorageMapping(String storageClass) {
    return mappingRepository
        .findEnabledMapping(storageClass, providerId)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No storage mapping found for storage class: " + storageClass));
  }

  private VolumeMetadata parseVolumeId(String volumeId) {
    String[] parts = volumeId.split("/");
    if (parts.length < 3) {
      throw new IllegalArgumentException("Invalid volume ID format: " + volumeId);
    }
    return new VolumeMetadata(parts[0], parts[1], parts[2]);
  }

  private SnapshotMetadata parseSnapshotId(String snapshotId) {
    String[] parts = snapshotId.split("@");
    if (parts.length != 2) {
      throw new IllegalArgumentException("Invalid snapshot ID format: " + snapshotId);
    }
    VolumeMetadata volumeMetadata = parseVolumeId(parts[0]);
    return new SnapshotMetadata(
        volumeMetadata.backendType(),
        volumeMetadata.poolName(),
        volumeMetadata.volumeName(),
        parts[1]);
  }

  record VolumeMetadata(String backendType, String poolName, String volumeName) {}

  record SnapshotMetadata(
      String backendType, String poolName, String volumeName, String snapshotName) {}
}
