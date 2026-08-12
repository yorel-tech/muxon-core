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
import java.util.List;
import org.libvirt.Connect;

/**
 * Interface for Libvirt storage backend implementations.
 *
 * <p>Each storage backend (Ceph RBD, LVM, ZFS) implements this interface to provide
 * backend-specific storage operations using libvirt storage pool APIs.
 */
public interface LibvirtStorageBackend {

  /**
   * Create a new volume in the storage backend.
   *
   * @param connection libvirt connection
   * @param request volume creation request
   * @param mapping provider storage mapping configuration
   * @return volume creation result
   */
  VolumeCreationResult createVolume(
      Connect connection, VolumeCreationRequest request, ProviderStorageMappingEntity mapping);

  /**
   * Delete a volume from the storage backend.
   *
   * @param connection libvirt connection
   * @param volumeId provider-specific volume identifier
   * @param metadata volume metadata
   * @return operation result
   */
  VolumeOperationResult deleteVolume(
      Connect connection, String volumeId, LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Resize a volume to a new size.
   *
   * @param connection libvirt connection
   * @param volumeId provider-specific volume identifier
   * @param newSizeBytes new size in bytes
   * @param metadata volume metadata
   * @return operation result
   */
  VolumeOperationResult resizeVolume(
      Connect connection,
      String volumeId,
      long newSizeBytes,
      LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Attach a volume to a VM.
   *
   * @param connection libvirt connection
   * @param request attachment request
   * @param metadata volume metadata
   * @return attachment result
   */
  VolumeAttachmentResult attachVolume(
      Connect connection,
      VolumeAttachmentRequest request,
      LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Detach a volume from a VM.
   *
   * @param connection libvirt connection
   * @param volumeId provider-specific volume identifier
   * @param resourceId resource identifier
   * @param metadata volume metadata
   * @return operation result
   */
  VolumeOperationResult detachVolume(
      Connect connection,
      String volumeId,
      String resourceId,
      LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Get volume information.
   *
   * @param connection libvirt connection
   * @param volumeId provider-specific volume identifier
   * @param metadata volume metadata
   * @return volume information
   */
  VolumeInfo getVolumeInfo(
      Connect connection, String volumeId, LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * List all volumes.
   *
   * @param connection libvirt connection
   * @param request list request
   * @return list of volume information
   */
  List<VolumeInfo> listVolumes(Connect connection, VolumeListRequest request);

  /**
   * Create a snapshot of a volume.
   *
   * @param connection libvirt connection
   * @param request snapshot creation request
   * @param metadata volume metadata
   * @return snapshot creation result
   */
  SnapshotCreationResult createSnapshot(
      Connect connection,
      SnapshotCreationRequest request,
      LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Delete a snapshot.
   *
   * @param connection libvirt connection
   * @param snapshotId provider-specific snapshot identifier
   * @param metadata snapshot metadata
   * @return operation result
   */
  SnapshotOperationResult deleteSnapshot(
      Connect connection, String snapshotId, LibvirtStorageProvider.SnapshotMetadata metadata);

  /**
   * Restore a volume from a snapshot.
   *
   * @param connection libvirt connection
   * @param request restore request
   * @param metadata snapshot metadata
   * @return volume creation result
   */
  VolumeCreationResult restoreFromSnapshot(
      Connect connection,
      SnapshotRestoreRequest request,
      LibvirtStorageProvider.SnapshotMetadata metadata);

  /**
   * Clone a volume from a snapshot (copy-on-write).
   *
   * @param connection libvirt connection
   * @param request clone request
   * @param metadata snapshot metadata
   * @return volume creation result
   */
  VolumeCreationResult cloneFromSnapshot(
      Connect connection,
      SnapshotCloneRequest request,
      LibvirtStorageProvider.SnapshotMetadata metadata);

  /**
   * List snapshots for a volume.
   *
   * @param connection libvirt connection
   * @param request list request
   * @param metadata volume metadata
   * @return list of snapshot information
   */
  List<SnapshotInfo> listSnapshots(
      Connect connection,
      SnapshotListRequest request,
      LibvirtStorageProvider.VolumeMetadata metadata);

  /**
   * Get backend capacity and usage information.
   *
   * @param connection libvirt connection
   * @return backend information
   */
  StorageCapabilities.StorageBackendInfo getBackendInfo(Connect connection);
}
