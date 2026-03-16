package com.onetattva.infron.core.services.storage;

import com.onetattva.infron.db.model.VolumeEntity;
import com.onetattva.infron.db.model.VolumeAttachmentEntity;
import com.onetattva.infron.db.repository.VolumeRepository;
import com.onetattva.infron.db.repository.VolumeAttachmentRepository;
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
 * Service for managing volume lifecycle operations.
 * <p>
 * Handles volume creation, deletion, resizing, and attachment/detachment
 * to resources (VMs, pods, containers). Validates storage class support
 * and enforces business rules such as preventing deletion of attached volumes.
 * </p>
 * <p>
 * Volume lifecycle: creating → available → attaching → in_use → detaching → available
 * </p>
 */
@Service
public class VolumeService {

    private static final Logger log = LoggerFactory.getLogger(VolumeService.class);

    private final VolumeRepository volumeRepository;
    private final VolumeAttachmentRepository volumeAttachmentRepository;
    private final StorageClassResolutionService storageClassResolutionService;

    public VolumeService(
            VolumeRepository volumeRepository,
            VolumeAttachmentRepository volumeAttachmentRepository,
            StorageClassResolutionService storageClassResolutionService) {
        this.volumeRepository = volumeRepository;
        this.volumeAttachmentRepository = volumeAttachmentRepository;
        this.storageClassResolutionService = storageClassResolutionService;
    }

    /**
     * Create a new volume.
     * <p>
     * Validates that the storage class is supported by the provider before
     * creating the volume. The volume is initially set to "creating" status
     * and should be transitioned to "available" by the provider once ready.
     * </p>
     *
     * @param volume volume entity with name, storage class, size, and provider
     * @return created volume entity
     * @throws IllegalArgumentException if storage class is not supported by provider
     */
    @Transactional
    public VolumeEntity createVolume(VolumeEntity volume) {
        log.info("Creating volume: name={}, storageClass={}, size={}", 
            volume.getName(), volume.getStorageClass(), volume.getSizeBytes());

        if (!storageClassResolutionService.isStorageClassSupported(
                volume.getStorageClass(), volume.getProviderId())) {
            throw new IllegalArgumentException(
                "Storage class " + volume.getStorageClass() + 
                " not supported by provider " + volume.getProviderId());
        }

        volume.setStatus("creating");
        return volumeRepository.save(volume);
    }

    @Transactional
    public void updateVolumeStatus(UUID volumeId, String status) {
        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(volumeId)
            .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));
        
        volume.setStatus(status);
        volumeRepository.save(volume);
        
        log.info("Updated volume {} status to {}", volumeId, status);
    }

    /**
     * Delete a volume (soft delete).
     * <p>
     * Marks the volume as deleted by setting deletedAt timestamp.
     * The volume must not be attached to any resource.
     * </p>
     *
     * @param volumeId volume identifier
     * @throws IllegalArgumentException if volume not found
     * @throws IllegalStateException if volume is currently attached
     */
    @Transactional
    public void deleteVolume(UUID volumeId) {
        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(volumeId)
            .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));

        if (volumeAttachmentRepository.existsByVolumeIdAndDetachedAtIsNull(volumeId)) {
            throw new IllegalStateException("Cannot delete volume that is attached");
        }

        volume.setDeletedAt(Instant.now());
        volume.setStatus("deleted");
        volumeRepository.save(volume);
        
        log.info("Deleted volume {}", volumeId);
    }

    /**
     * Resize a volume to a larger size.
     * <p>
     * Expands the volume while preserving existing data. The new size must
     * be larger than the current size. The volume is set to "resizing" status
     * and should be transitioned back to its previous status by the provider.
     * </p>
     *
     * @param volumeId volume identifier
     * @param newSizeBytes new size in bytes (must be larger than current)
     * @throws IllegalArgumentException if volume not found or new size is not larger
     */
    @Transactional
    public void resizeVolume(UUID volumeId, long newSizeBytes) {
        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(volumeId)
            .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));

        if (newSizeBytes <= volume.getSizeBytes()) {
            throw new IllegalArgumentException("New size must be larger than current size");
        }

        volume.setSizeBytes(newSizeBytes);
        volume.setStatus("resizing");
        volumeRepository.save(volume);
        
        log.info("Resizing volume {} to {} bytes", volumeId, newSizeBytes);
    }

    public Optional<VolumeEntity> getVolume(UUID volumeId) {
        return volumeRepository.findByIdAndDeletedAtIsNull(volumeId);
    }

    public Page<VolumeEntity> listVolumesByWorkspace(UUID workspaceId, Pageable pageable) {
        return volumeRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId, pageable);
    }

    public Page<VolumeEntity> listVolumesByProvider(UUID providerId, Pageable pageable) {
        return volumeRepository.findByProviderIdAndDeletedAtIsNull(providerId, pageable);
    }

    public List<VolumeEntity> getVolumesByWorkspace(UUID workspaceId) {
        return volumeRepository.findByWorkspaceIdAndDeletedAtIsNull(workspaceId);
    }

    /**
     * Attach a volume to a resource (VM, pod, container).
     * <p>
     * Creates an attachment record and transitions the volume to "in_use" status.
     * A volume can only be attached to one resource at a time.
     * </p>
     *
     * @param volumeId volume identifier
     * @param resourceType resource type (e.g., "vm", "pod", "container")
     * @param resourceId resource identifier
     * @param device device path (e.g., "/dev/vdb", "pvc-name")
     * @return created attachment entity
     * @throws IllegalArgumentException if volume not found
     * @throws IllegalStateException if volume is already attached
     */
    @Transactional
    public VolumeAttachmentEntity attachVolume(
            UUID volumeId, String resourceType, UUID resourceId, String device) {
        
        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(volumeId)
            .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));

        if (volumeAttachmentRepository.existsByVolumeIdAndDetachedAtIsNull(volumeId)) {
            throw new IllegalStateException("Volume is already attached");
        }

        VolumeAttachmentEntity attachment = new VolumeAttachmentEntity();
        attachment.setVolumeId(volumeId);
        attachment.setResourceType(resourceType);
        attachment.setResourceId(resourceId);
        attachment.setDevice(device);
        
        VolumeAttachmentEntity saved = volumeAttachmentRepository.save(attachment);

        volume.setStatus("in_use");
        volumeRepository.save(volume);
        
        log.info("Attached volume {} to {} {}", volumeId, resourceType, resourceId);
        
        return saved;
    }

    /**
     * Detach a volume from a resource.
     * <p>
     * Marks the attachment as detached and transitions the volume back to
     * "available" status. The attachment record is preserved for audit purposes.
     * </p>
     *
     * @param volumeId volume identifier
     * @param resourceId resource identifier
     * @throws IllegalArgumentException if volume or attachment not found
     */
    @Transactional
    public void detachVolume(UUID volumeId, UUID resourceId) {
        VolumeAttachmentEntity attachment = volumeAttachmentRepository
            .findByVolumeIdAndResourceIdAndDetachedAtIsNull(volumeId, resourceId)
            .orElseThrow(() -> new IllegalArgumentException(
                "No active attachment found for volume " + volumeId + " and resource " + resourceId));

        attachment.setDetachedAt(Instant.now());
        volumeAttachmentRepository.save(attachment);

        VolumeEntity volume = volumeRepository.findByIdAndDeletedAtIsNull(volumeId)
            .orElseThrow(() -> new IllegalArgumentException("Volume not found: " + volumeId));
        
        volume.setStatus("available");
        volumeRepository.save(volume);
        
        log.info("Detached volume {} from resource {}", volumeId, resourceId);
    }

    public List<VolumeAttachmentEntity> getVolumeAttachments(UUID volumeId) {
        return volumeAttachmentRepository.findByVolumeIdAndDetachedAtIsNull(volumeId);
    }

    public List<VolumeAttachmentEntity> getResourceAttachments(String resourceType, UUID resourceId) {
        return volumeAttachmentRepository.findByResourceTypeAndResourceIdAndDetachedAtIsNull(
            resourceType, resourceId);
    }
}
