package com.yorel.muxon.worker.executors;

import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.api.model.EntityType;
import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.providers.proxmox.ProxmoxStorageUploader;
import com.yorel.muxon.spi.queue.CommandMessage;
import com.yorel.muxon.spi.queue.CommandQueue;
import com.yorel.muxon.spi.queue.EntityEventQueue;
import com.yorel.muxon.spi.queue.ProviderQueueCommands;
import com.yorel.muxon.spi.queue.ProviderQueueMetadataKeys;
import com.yorel.muxon.db.model.ContentItemDistributionEntity;
import com.yorel.muxon.db.model.ContentItemEntity;
import com.yorel.muxon.db.model.ProviderEntity;
import com.yorel.muxon.db.model.ProviderStorageEntity;
import com.yorel.muxon.db.repository.ContentItemDistributionRepository;
import com.yorel.muxon.db.repository.ContentItemRepository;
import com.yorel.muxon.db.repository.JobRepository;
import com.yorel.muxon.db.repository.ProviderRepository;
import com.yorel.muxon.db.repository.ProviderStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Executes content-library scoped commands claimed from {@link CommandQueue}.
 *
 * <p>Polling is done by an external service (OSS orchestrator poller or enterprise worker-service).
 */
@Service
public class ContentTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(ContentTaskExecutor.class);

    @Autowired private CommandQueue commandQueue;
    @Autowired private ProviderRepository providerRepository;
    @Autowired private ProviderStorageRepository providerStorageRepository;
    @Autowired private ProxmoxStorageUploader proxmoxStorageUploader;
    @Autowired private ContentItemRepository contentItemRepository;
    @Autowired private ContentItemDistributionRepository contentItemDistributionRepository;
    @Autowired private EntityEventQueue entityEventQueue;
    @Autowired private JobRepository jobRepository;

    @Transactional
    public void execute(CommandMessage entry) {
        String queueType = entry.queueType();
        if (ProviderQueueCommands.CONTENT_DATACENTER_REPLICATE.equals(queueType)) {
            processContentDatacenterReplicate(entry);
            return;
        }

        // This executor only knows content-library related queue types.
        logger.warn("Unknown content queue type {}; marking completed. commandId={}", queueType, entry.id());
        commandQueue.markCompleted(entry.id());
    }

    private void processContentDatacenterReplicate(CommandMessage entry) {
        UUID providerId = entry.entityId();
        UUID jobId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.JOB_ID));
        UUID libraryId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.LIBRARY_ID));
        UUID datacenterId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.DATACENTER_ID));
        UUID distributionId = parseUuid(entry.metadata().get(ProviderQueueMetadataKeys.DISTRIBUTION_ID));
        String artifactRootStr = entry.metadata().get(ProviderQueueMetadataKeys.ARTIFACT_ROOT);
        String storagePoolIdsRaw = entry.metadata().get(ProviderQueueMetadataKeys.STORAGE_POOL_IDS);
        int execTimeoutSec =
                parseIntOrDefault(entry.metadata().get(ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS), 300);

        logger.info(
                "Content datacenter replicate claimed: commandId={}, providerId={}, jobId={}, libraryId={}, datacenterId={}, distributionId={}",
                entry.id(),
                providerId,
                jobId,
                libraryId,
                datacenterId,
                distributionId);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Content datacenter replicate config: commandId={}, artifactRoot={}, storagePoolIds={}, executionTimeoutSec={}",
                    entry.id(),
                    artifactRootStr,
                    storagePoolIdsRaw,
                    execTimeoutSec);
        }

        if (jobId == null
                || libraryId == null
                || datacenterId == null
                || distributionId == null
                || artifactRootStr == null
                || artifactRootStr.isBlank()
                || storagePoolIdsRaw == null
                || storagePoolIdsRaw.isBlank()) {
            String msg =
                    "Content datacenter replicate missing required metadata (jobId, libraryId, datacenterId, distributionId, artifactRoot, storagePoolIds)";
            failContentDatacenterReplicateJob(jobId, msg);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        ProviderEntity entity = providerRepository.findById(providerId).orElse(null);
        if (entity == null) {
            String msg = "Provider not found: " + providerId;
            failContentDatacenterReplicateJob(jobId, msg);
            publishDistributionFailed(distributionId, entry.id(), msg, 0);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }
        if (!ProviderType.PROXMOX.equals(entity.getType())) {
            String msg =
                    "Content datacenter replicate is only supported for Proxmox providers (got " + entity.getType() + ")";
            failContentDatacenterReplicateJob(jobId, msg);
            publishDistributionFailed(distributionId, entry.id(), msg, 0);
            commandQueue.markFailed(entry.id(), msg);
            return;
        }

        markContentDatacenterReplicateJobRunning(jobId);

        try {
            Path artifactRoot = Path.of(artifactRootStr).toAbsolutePath().normalize();
            ProxmoxStorageUploader.PveAuthSession session =
                    proxmoxStorageUploader.authenticate(entity.getEndpoint(), entity.getCredentials());

            List<ContentItemEntity> items = contentItemRepository.findByLibraryId(libraryId);
            List<ContentItemEntity> available =
                    items.stream().filter(i -> "available".equalsIgnoreCase(i.getContentStatus())).toList();
            Map<UUID, String> distributionItemStatus =
                    contentItemDistributionRepository.findByDistributionIdOrderByContentItemId(distributionId).stream()
                            .collect(
                                    Collectors.toUnmodifiableMap(
                                            ContentItemDistributionEntity::getContentItemId,
                                            ContentItemDistributionEntity::getStatus,
                                            (a, b) -> b));
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Content datacenter replicate upload phase: providerId={}, providerType={}, endpoint={}, libraryItemCount={}, availableItemCount={}, distributionRowCount={}",
                        providerId,
                        entity.getType(),
                        entity.getEndpoint(),
                        items.size(),
                        available.size(),
                        distributionItemStatus.size());
            }

            List<ProviderStorageEntity> poolEntities = new ArrayList<>();
            for (String poolIdRaw : storagePoolIdsRaw.split(",")) {
                String storagePoolId = poolIdRaw.trim();
                if (storagePoolId.isEmpty()) {
                    continue;
                }
                ProviderStorageEntity ps =
                        providerStorageRepository
                                .findByProviderIdAndExternalId(providerId, storagePoolId)
                                .orElseThrow(
                                        () ->
                                                new IllegalStateException(
                                                        "Provider storage not found for provider "
                                                                + providerId
                                                                + " storage "
                                                                + storagePoolId));
                String nodeName = ps.getNodeId();
                if (nodeName == null || nodeName.isBlank()) {
                    throw new IllegalStateException(
                            "Provider storage " + storagePoolId + " has no node id (run inventory sync)");
                }
                poolEntities.add(ps);
            }
            if (poolEntities.isEmpty()) {
                throw new IllegalStateException("No valid storage pools in metadata");
            }

            int uploadCount = 0;
            int n = available.size();
            int done = 0;
            if (n == 0) {
                completeContentDatacenterReplicateJob(jobId, libraryId, datacenterId, 0);
                publishDistributionCompleted(distributionId, entry.id());
                commandQueue.markCompleted(entry.id());
                return;
            }
            for (ContentItemEntity item : available) {
                String rel = item.getProviderRelativePath();
                if (rel == null || rel.isBlank()) {
                    throw new IllegalStateException("Content item " + item.getId() + " has no provider relative path");
                }
                Path src = artifactRoot.resolve(rel).normalize();
                if (!src.startsWith(artifactRoot)) {
                    throw new IllegalStateException("Refusing to read outside artifact root: " + src);
                }
                if (!Files.isRegularFile(src)) {
                    throw new IllegalStateException("Missing artifact file for item " + item.getId() + " at " + src);
                }
                String leaf = src.getFileName().toString();
                long sizeBytes = Files.size(src);

                String rowStatus = distributionItemStatus.get(item.getId());
                if (rowStatus != null && "READY".equalsIgnoreCase(rowStatus)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(
                                "Skipping Proxmox upload (already READY for this distribution): contentItemId={}, path={}",
                                item.getId(),
                                src);
                    }
                    done++;
                    continue;
                }

                if (skipProxmoxVmTemplateMetadataUpload(item.getContentType(), leaf)) {
                    if (logger.isInfoEnabled()) {
                        logger.info(
                                "Skipping Proxmox upload for VM template metadata file (not accepted as import): contentItemId={}, leaf={}, path={}",
                                item.getId(),
                                leaf,
                                src);
                    }
                    done++;
                    int progressPercent = n == 0 ? 100 : (done * 100 / n);
                    publishDistributionItemUpdated(
                            distributionId,
                            entry.id(),
                            item.getId(),
                            "READY",
                            sizeBytes,
                            true,
                            null,
                            progressPercent);
                    continue;
                }

                for (ProviderStorageEntity ps : poolEntities) {
                    String storagePoolId = ps.getExternalId().trim();
                    String nodeName = ps.getNodeId();
                    String proxmoxContent =
                            ProxmoxStorageUploader.resolveProxmoxContentTypeForItem(item.getContentType(), leaf);
                    ensureStorageAcceptsProxmoxContent(ps, proxmoxContent);
                    String uploadName =
                            ProxmoxStorageUploader.encodeMuxonUploadFilename(libraryId, item.getId(), leaf);
                    String upid =
                            proxmoxStorageUploader.upload(
                                    entity.getEndpoint(),
                                    session,
                                    nodeName,
                                    storagePoolId,
                                    proxmoxContent,
                                    uploadName,
                                    src,
                                    item.getContentType());
                    proxmoxStorageUploader.waitForTask(entity.getEndpoint(), session, nodeName, upid, execTimeoutSec);
                    uploadCount++;
                }

                done++;
                int progressPercent = n == 0 ? 100 : (done * 100 / n);
                publishDistributionItemUpdated(
                        distributionId,
                        entry.id(),
                        item.getId(),
                        "READY",
                        sizeBytes,
                        true,
                        null,
                        progressPercent);
            }

            completeContentDatacenterReplicateJob(jobId, libraryId, datacenterId, uploadCount);
            publishDistributionCompleted(distributionId, entry.id());
            logger.info(
                    "Content datacenter replicate completed: commandId={}, jobId={}, uploadCount={}",
                    entry.id(),
                    jobId,
                    uploadCount);
            commandQueue.markCompleted(entry.id());
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "Content datacenter replicate failed";
            logger.error("Content datacenter replicate failed: providerId={}, jobId={}", providerId, jobId, e);
            failContentDatacenterReplicateJob(jobId, msg);
            publishDistributionFailed(distributionId, entry.id(), msg, 0);
            commandQueue.markFailed(entry.id(), msg);
        }
    }

    private void publishDistributionItemUpdated(
            UUID distributionId,
            UUID taskId,
            UUID contentItemId,
            String status,
            long sizeBytes,
            boolean checksumVerified,
            String errorMessage,
            int progressPercent) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("contentItemId", contentItemId.toString());
            payload.put("status", status);
            payload.put("sizeBytes", sizeBytes);
            payload.put("checksumVerified", checksumVerified);
            if (errorMessage != null) {
                payload.put("errorMessage", errorMessage);
            }
            payload.put("progressPercent", progressPercent);
            entityEventQueue.publishEntityEvent(
                    EntityType.CONTENT_LIBRARY,
                    distributionId,
                    EntityEventQueue.EntityEventTypes.CL_DISTRIBUTION_ITEM_UPDATED,
                    taskId,
                    payload);
        } catch (Exception ex) {
            logger.warn("Failed to publish CL_DISTRIBUTION_ITEM_UPDATED: {}", ex.getMessage());
        }
    }

    private void publishDistributionCompleted(UUID distributionId, UUID taskId) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("progressPercent", 100);
            payload.put("lastReplicatedAt", Instant.now().toString());
            entityEventQueue.publishEntityEvent(
                    EntityType.CONTENT_LIBRARY,
                    distributionId,
                    EntityEventQueue.EntityEventTypes.CL_DISTRIBUTION_COMPLETED,
                    taskId,
                    payload);
        } catch (Exception ex) {
            logger.warn("Failed to publish CL_DISTRIBUTION_COMPLETED: {}", ex.getMessage());
        }
    }

    private void publishDistributionFailed(UUID distributionId, UUID taskId, String errorMessage, int progressPercent) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("errorMessage", errorMessage != null ? errorMessage : "unknown");
            payload.put("progressPercent", progressPercent);
            entityEventQueue.publishEntityEvent(
                    EntityType.CONTENT_LIBRARY,
                    distributionId,
                    EntityEventQueue.EntityEventTypes.CL_DISTRIBUTION_FAILED,
                    taskId,
                    payload);
        } catch (Exception ex) {
            logger.warn("Failed to publish CL_DISTRIBUTION_FAILED: {}", ex.getMessage());
        }
    }

    private static void ensureStorageAcceptsProxmoxContent(ProviderStorageEntity ps, String proxmoxContent) {
        if (ps.getCapabilities() == null) {
            return;
        }
        Object raw = ps.getCapabilities().get("content_types");
        if (raw == null) {
            return;
        }
        List<String> types = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    types.add(o.toString().trim());
                }
            }
        } else {
            for (String part : raw.toString().split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    types.add(t);
                }
            }
        }
        if (types.isEmpty()) {
            return;
        }
        String want = proxmoxContent.trim().toLowerCase(Locale.ROOT);
        boolean ok = types.stream().anyMatch(t -> t.toLowerCase(Locale.ROOT).equals(want));
        if (!ok) {
            throw new IllegalArgumentException(
                    "Proxmox storage \""
                            + ps.getExternalId()
                            + "\" does not accept content type "
                            + proxmoxContent
                            + "; allowed: "
                            + types);
        }
    }

    private static boolean skipProxmoxVmTemplateMetadataUpload(String catalogContentType, String leafFilename) {
        if (catalogContentType == null || leafFilename == null) {
            return false;
        }
        if (!"vm_template".equalsIgnoreCase(catalogContentType.trim())) {
            return false;
        }
        return "template.json".equalsIgnoreCase(leafFilename.trim());
    }

    private void markContentDatacenterReplicateJobRunning(UUID jobId) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            if (job.getStatus() != JobStatus.PENDING) {
                logger.warn(
                        "Content datacenter replicate job {} not in PENDING (was {}); still marking RUNNING",
                        jobId,
                        job.getStatus());
            }
            job.setStatus(JobStatus.RUNNING);
            job.setStartedAt(Instant.now());
            job.setLastHeartbeatAt(Instant.now());
            job.setCurrentStep("Uploading content library to Proxmox storage");
            jobRepository.save(job);
            logger.info("Content datacenter replicate job marked RUNNING: jobId={}", jobId);
        });
    }

    private void completeContentDatacenterReplicateJob(UUID jobId, UUID libraryId, UUID datacenterId, int uploadCount) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setProgressPercentage(100);
            job.setCurrentStep(null);
            job.setResult(
                    String.format(
                            Locale.US,
                            "{\"libraryId\":\"%s\",\"datacenterId\":\"%s\",\"uploadCount\":%d}",
                            libraryId,
                            datacenterId,
                            uploadCount));
            jobRepository.save(job);
            logger.info("Content datacenter replicate job COMPLETED: jobId={}, uploadCount={}", jobId, uploadCount);
        });
    }

    private void failContentDatacenterReplicateJob(UUID jobId, String message) {
        if (jobId == null) {
            return;
        }
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(message);
            job.setCurrentStep(null);
            jobRepository.save(job);
            logger.info("Content datacenter replicate job FAILED: jobId={}, message={}", jobId, message);
        });
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int parseIntOrDefault(String raw, int def) {
        if (raw == null || raw.isBlank()) {
            return def;
        }
        try {
            int v = Integer.parseInt(raw.trim());
            return v > 0 ? v : def;
        } catch (NumberFormatException e) {
            return def;
        }
    }
}

