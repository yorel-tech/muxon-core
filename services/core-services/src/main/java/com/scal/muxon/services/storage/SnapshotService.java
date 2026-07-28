package com.scal.muxon.services.storage;

import com.scal.muxon.db.model.SnapshotEntity;
import com.scal.muxon.db.model.VolumeEntity;
import com.scal.muxon.db.repository.SnapshotRepository;
import com.scal.muxon.db.repository.VolumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing snapshot lifecycle operations.
 * <p>
 * Handles snapshot creation, deletion, restore, and clone operations.
 * Supports immutable snapshots with retention periods for compliance
 * and WORM (Write Once Read Many) requirements.
 * </p>
 * <p>
 * Snapshot lifecycle: creating → available → deleting → deleted
 * </p>
 */
@Service
public class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private final SnapshotRepository snapshotRepository;
    private final VolumeRepository volumeRepository;

    public SnapshotService(
            SnapshotRepository snapshotRepository,
            VolumeRepository volumeRepository) {
        this.snapshotRepository = snapshotRepository;
        this.volumeRepository = volumeRepository;
    }

    /**
     * Create a point-in-time snapshot of a volume.
     * <p>
     * Captures the current state of the volume. The snapshot size is set
     * to match the volume size. The snapshot is initially in "creating" status
     * and should be transitioned to "available" by the provider once ready.
     * </p>
     *
     * @param snapshot snapshot entity with volume ID, name, and optional retention settings
     * @return created snapshot entity
     * @throws IllegalArgumentException if volume not found
     */
    @Transactional
    public SnapshotEntity createSnapshot(SnapshotEntity snapshot) {
        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(snapshot.getVolumeId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Volume not found: " + snapshot.getVolumeId()));

        snapshot.setStatus("creating");
        snapshot.setSizeBytes(volume.getSizeBytes());
        
        SnapshotEntity saved = snapshotRepository.save(snapshot);
        
        log.info("Creating snapshot: name={}, volumeId={}", 
            snapshot.getName(), snapshot.getVolumeId());
        
        return saved;
    }

    @Transactional
    public void updateSnapshotStatus(UUID snapshotId, String status) {
        SnapshotEntity snapshot = snapshotRepository.findByIdAndDeletedAtIsNull(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));
        
        snapshot.setStatus(status);
        snapshotRepository.save(snapshot);
        
        log.info("Updated snapshot {} status to {}", snapshotId, status);
    }

    /**
     * Delete a snapshot (soft delete).
     * <p>
     * Marks the snapshot as deleted. Immutable snapshots cannot be deleted
     * before their retention period expires (compliance/WORM enforcement).
     * </p>
     *
     * @param snapshotId snapshot identifier
     * @throws IllegalArgumentException if snapshot not found
     * @throws IllegalStateException if immutable snapshot retention period has not expired
     */
    @Transactional
    public void deleteSnapshot(UUID snapshotId) {
        SnapshotEntity snapshot = snapshotRepository.findByIdAndDeletedAtIsNull(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        if (snapshot.getImmutable() && snapshot.getRetentionUntil() != null 
                && snapshot.getRetentionUntil().isAfter(Instant.now())) {
            throw new IllegalStateException(
                "Cannot delete immutable snapshot before retention period expires");
        }

        snapshot.setDeletedAt(Instant.now());
        snapshot.setStatus("deleted");
        snapshotRepository.save(snapshot);
        
        log.info("Deleted snapshot {}", snapshotId);
    }

    public Optional<SnapshotEntity> getSnapshot(UUID snapshotId) {
        return snapshotRepository.findByIdAndDeletedAtIsNull(snapshotId);
    }

    public Page<SnapshotEntity> listSnapshotsByVolume(UUID volumeId, Pageable pageable) {
        return snapshotRepository.findByVolumeIdAndDeletedAtIsNull(volumeId, pageable);
    }

    public List<SnapshotEntity> getSnapshotsByVolume(UUID volumeId) {
        return snapshotRepository.findByVolumeIdAndDeletedAtIsNull(volumeId);
    }

    /**
     * Restore a new volume from a snapshot.
     * <p>
     * Creates a new volume with the contents of the snapshot. The new volume
     * inherits properties from the source volume (workspace, encryption, etc.)
     * but can use a different storage class if specified.
     * </p>
     *
     * @param snapshotId snapshot identifier
     * @param volumeName name for the new volume
     * @param storageClass storage class for new volume (null to use source volume's class)
     * @return created volume entity in "creating" status
     * @throws IllegalArgumentException if snapshot or source volume not found
     */
    @Transactional
    public VolumeEntity restoreFromSnapshot(UUID snapshotId, String volumeName, String storageClass) {
        SnapshotEntity snapshot = snapshotRepository.findByIdAndDeletedAtIsNull(snapshotId)
            .orElseThrow(() -> new IllegalArgumentException("Snapshot not found: " + snapshotId));

        VolumeEntity sourceVolume = volumeRepository.findByIdAndDeletedAtIsNull(snapshot.getVolumeId())
            .orElseThrow(() -> new IllegalArgumentException(
                "Source volume not found: " + snapshot.getVolumeId()));

        VolumeEntity newVolume = new VolumeEntity();
        newVolume.setName(volumeName);
        newVolume.setWorkspaceId(sourceVolume.getWorkspaceId());
        newVolume.setStorageClass(storageClass != null ? storageClass : sourceVolume.getStorageClass());
        newVolume.setSizeBytes(snapshot.getSizeBytes());
        newVolume.setProviderId(sourceVolume.getProviderId());
        newVolume.setEncrypted(sourceVolume.getEncrypted());
        newVolume.setThinProvisioned(sourceVolume.getThinProvisioned());
        newVolume.setStatus("creating");
        newVolume.setProviderVolumeId("pending");
        
        VolumeEntity saved = volumeRepository.save(newVolume);
        
        log.info("Restoring volume {} from snapshot {}", volumeName, snapshotId);
        
        return saved;
    }

    /**
     * Clone a new volume from a snapshot using copy-on-write.
     * <p>
     * Creates a new volume that initially shares storage with the snapshot
     * but diverges as writes occur. This is more efficient than full restore
     * for providers that support CoW (e.g., Ceph RBD, ZFS).
     * </p>
     * <p>
     * Note: Currently implemented as an alias to restoreFromSnapshot.
     * Provider-specific implementations should optimize this for CoW.
     * </p>
     *
     * @param snapshotId snapshot identifier
     * @param volumeName name for the new volume
     * @param storageClass storage class for new volume (null to use source volume's class)
     * @return created volume entity in "creating" status
     */
    @Transactional
    public VolumeEntity cloneFromSnapshot(UUID snapshotId, String volumeName, String storageClass) {
        return restoreFromSnapshot(snapshotId, volumeName, storageClass);
    }

    public List<SnapshotEntity> findExpiredImmutableSnapshots() {
        return snapshotRepository.findExpiredImmutableSnapshots(Instant.now());
    }
}
