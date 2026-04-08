package com.onetattva.infron.core.services.content;

import com.onetattva.infron.api.dto.TaskCreateRequest;
import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.api.model.ContentReplicateResponse;
import com.onetattva.infron.core.common.EntityNotFoundException;
import com.onetattva.infron.core.services.storage.scheduler.StorageSchedulerService;
import com.onetattva.infron.core.services.task.TaskOrchestrationService;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import com.onetattva.infron.core.spi.queue.ProviderQueueMetadataKeys;
import com.onetattva.infron.db.model.ContentItemEntity;
import com.onetattva.infron.db.model.ContentLibraryDatacenterEntity;
import com.onetattva.infron.db.model.ContentLibraryEntity;
import com.onetattva.infron.db.model.ContentStorageEntity;
import com.onetattva.infron.db.model.DatacenterEntity;
import com.onetattva.infron.db.model.JobEntity;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ContentItemRepository;
import com.onetattva.infron.db.repository.ContentLibraryDatacenterRepository;
import com.onetattva.infron.db.repository.ContentLibraryRepository;
import com.onetattva.infron.db.repository.ContentStorageRepository;
import com.onetattva.infron.db.repository.DatacenterRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Replicate remote library artifacts into the content store (platform remote libraries only),
 * and copy artifacts from the content store into provider storage pools that match the published storage class.
 */
@Service
public class ContentLibraryReplicateService {

    private static final Logger log = LoggerFactory.getLogger(ContentLibraryReplicateService.class);

    /** Populated on discovered dir/netfs libvirt pools; absolute path on the host running core-services (or shared mount). */
    public static final String PROVIDER_STORAGE_HOST_PATH_CAP = "host_path";

    private static final int DEFAULT_TASK_TIMEOUT_SECONDS = 3600;

    private final ContentLibraryRepository contentLibraryRepository;
    private final ContentItemRepository contentItemRepository;
    private final ContentLibraryDatacenterRepository contentLibraryDatacenterRepository;
    private final ContentStorageRepository contentStorageRepository;
    private final DatacenterRepository datacenterRepository;
    private final TaskOrchestrationService taskOrchestrationService;
    private final ContentLibraryProviderPathBuilder contentLibraryProviderPathBuilder;
    private final ContentStoragePathResolver contentStoragePathResolver;
    private final ContentStorageS3Uploader contentStorageS3Uploader;
    private final StorageSchedulerService storageSchedulerService;
    private final CommandQueue commandQueue;

    public ContentLibraryReplicateService(
            ContentLibraryRepository contentLibraryRepository,
            ContentItemRepository contentItemRepository,
            ContentLibraryDatacenterRepository contentLibraryDatacenterRepository,
            ContentStorageRepository contentStorageRepository,
            DatacenterRepository datacenterRepository,
            TaskOrchestrationService taskOrchestrationService,
            ContentLibraryProviderPathBuilder contentLibraryProviderPathBuilder,
            ContentStoragePathResolver contentStoragePathResolver,
            ContentStorageS3Uploader contentStorageS3Uploader,
            StorageSchedulerService storageSchedulerService,
            CommandQueue commandQueue) {
        this.contentLibraryRepository = contentLibraryRepository;
        this.contentItemRepository = contentItemRepository;
        this.contentLibraryDatacenterRepository = contentLibraryDatacenterRepository;
        this.contentStorageRepository = contentStorageRepository;
        this.datacenterRepository = datacenterRepository;
        this.taskOrchestrationService = taskOrchestrationService;
        this.contentLibraryProviderPathBuilder = contentLibraryProviderPathBuilder;
        this.contentStoragePathResolver = contentStoragePathResolver;
        this.contentStorageS3Uploader = contentStorageS3Uploader;
        this.storageSchedulerService = storageSchedulerService;
        this.commandQueue = commandQueue;
    }

    @Transactional
    public ContentReplicateResponse replicateRemoteLibraryToContentStore(UUID libraryId) {
        ContentLibraryEntity library = contentLibraryRepository.findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));
        if (log.isDebugEnabled()) {
            log.debug(
                    "Content library replicate-to-store: libraryId={}, type={}, tenantId={}, contentStorageId={}, "
                            + "sourceConfigKeys={}",
                    libraryId,
                    library.getLibraryType(),
                    library.getTenantId(),
                    library.getContentStorageId(),
                    library.getSourceConfig() != null ? library.getSourceConfig().keySet() : java.util.Set.of());
        }
        if (!"remote".equals(library.getLibraryType().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Replicate to content store applies only to remote libraries");
        }
        if (!ContentLibraryService.SYSTEM_TENANT_ID.equals(library.getTenantId())) {
            throw new IllegalArgumentException("Remote library replicate is only supported for platform libraries");
        }

        TaskCreateRequest request = new TaskCreateRequest();
        request.setOperation(JobType.CONTENT_LIBRARY_REPLICATE);
        request.setEntityType(EntityType.PROVIDER);
        request.setEntityId(libraryId);
        request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
        request.setParameters(Map.of("libraryId", libraryId.toString()));
        request.setMetadata(Map.of("kind", "CONTENT_LIBRARY_REPLICATE", "libraryId", libraryId.toString()));
        JobEntity job = taskOrchestrationService.createTask(request);

        List<ContentItemEntity> items = contentItemRepository.findByLibraryId(libraryId);
        log.debug(
                "Content library replicate-to-store: marking {} items replicating, jobId={}",
                items.size(),
                job.getId());
        for (ContentItemEntity item : items) {
            item.setContentStatus("replicating");
            contentItemRepository.save(item);
        }

        taskOrchestrationService.startTask(job.getId());

        for (ContentItemEntity item : items) {
            item.setContentStatus("available");
            item.setLastReplicatedAt(Instant.now());
            contentLibraryProviderPathBuilder.applyProviderPaths(item);
            contentItemRepository.save(item);
        }

        taskOrchestrationService.completeTask(
                job.getId(), Map.of("libraryId", libraryId.toString(), "replicatedCount", items.size()));

        ContentReplicateResponse response = new ContentReplicateResponse();
        response.setLibraryId(libraryId);
        response.setTaskId(job.getId());
        response.setStatus(ContentReplicateResponse.StatusEnum.ACCEPTED);
        response.setMessage("Replicate to content store accepted");
        return response;
    }

    /**
     * Copies each {@code available} item under the same relative paths as in content-storage into every discovered
     * provider pool that matches the published storage class and exposes {@value #PROVIDER_STORAGE_HOST_PATH_CAP}.
     */
    public ContentReplicateResponse replicateToProviderStorage(UUID libraryId, UUID datacenterId) {
        ContentLibraryDatacenterEntity mapping = contentLibraryDatacenterRepository
                .findByLibraryIdAndDatacenterId(libraryId, datacenterId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Library is not published to datacenter: " + datacenterId));

        String storageClassName = mapping.getStorageClassName();
        if (log.isDebugEnabled()) {
            log.debug(
                    "Content library datacenter replicate start: libraryId={}, datacenterId={}, storageClassName={}",
                    libraryId,
                    datacenterId,
                    storageClassName);
        }
        if (storageClassName == null || storageClassName.isBlank()) {
            throw new IllegalArgumentException(
                    "This publish record has no storage class. Unpublish and publish again, choosing a datacenter "
                            + "storage class.");
        }

        ContentLibraryEntity library = contentLibraryRepository
                .findById(libraryId)
                .orElseThrow(() -> new EntityNotFoundException("Content library not found: " + libraryId));

        DatacenterEntity datacenter = datacenterRepository
                .findByIdWithNodeClusterAndProvider(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + datacenterId));
        if (datacenter.getNodeCluster() == null || datacenter.getNodeCluster().getProvider() == null) {
            throw new IllegalStateException("Datacenter is not backed by a provider cluster");
        }
        UUID providerId = datacenter.getNodeCluster().getProvider().getId();

        List<ProviderStorageEntity> matched =
                storageSchedulerService.findMatchingStoragePoolsForProvider(storageClassName, providerId);
        List<ProviderStorageEntity> scoped = matched.stream()
                .filter(ps -> ps.getDatacenterId() == null || datacenterId.equals(ps.getDatacenterId()))
                .toList();

        if (scoped.isEmpty()) {
            throw new IllegalArgumentException(
                    "No provider storage pools for storage class \""
                            + storageClassName
                            + "\" match this datacenter and provider.");
        }

        ProviderEntity provider = datacenter.getNodeCluster().getProvider();
        log.debug(
                "Content library datacenter replicate path: libraryId={}, datacenterId={}, providerId={}, "
                        + "providerType={}, matchedPoolCount={}",
                libraryId,
                datacenterId,
                provider.getId(),
                provider.getType(),
                scoped.size());
        if (ProviderType.PROXMOX.equals(provider.getType())) {
            return enqueueProxmoxDatacenterReplicate(
                    library,
                    mapping,
                    libraryId,
                    datacenterId,
                    storageClassName,
                    providerId,
                    scoped);
        }

        Set<Path> poolRoots = new LinkedHashSet<>();
        for (ProviderStorageEntity ps : scoped) {
            String hostPath = hostPathFrom(ps);
            if (hostPath != null && !hostPath.isBlank()) {
                poolRoots.add(Path.of(hostPath).toAbsolutePath().normalize());
            }
        }
        if (poolRoots.isEmpty()) {
            throw new IllegalArgumentException(
                    "No provider storage pools for storage class \""
                            + storageClassName
                            + "\" expose capabilities."
                            + PROVIDER_STORAGE_HOST_PATH_CAP
                            + " (filesystem path). Run storage discovery; libvirt dir pools publish this path. "
                            + "For Proxmox providers, replication is queued to the orchestrator (no host_path required). "
                            + "Pools that match only by type (e.g. RBD) cannot receive filesystem replication from "
                            + "core-services.");
        }
        log.debug(
                "Content library filesystem replicate: libraryId={}, datacenterId={}, poolRootCount={}, poolRoots={}",
                libraryId,
                datacenterId,
                poolRoots.size(),
                poolRoots);

        TaskCreateRequest request = new TaskCreateRequest();
        request.setOperation(JobType.CONTENT_DATACENTER_REPLICATE);
        request.setEntityType(EntityType.PROVIDER);
        request.setEntityId(libraryId);
        request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
        request.setParameters(Map.of(
                "libraryId", libraryId.toString(),
                "datacenterId", datacenterId.toString(),
                "storageClassName", storageClassName));
        request.setMetadata(Map.of(
                "kind", "CONTENT_DATACENTER_REPLICATE",
                "libraryId", libraryId.toString(),
                "datacenterId", datacenterId.toString(),
                "storageClassName", storageClassName));
        JobEntity job = taskOrchestrationService.createTask(request);

        mapping.setReplicateStatus("replicating");
        contentLibraryDatacenterRepository.save(mapping);

        taskOrchestrationService.startTask(job.getId());

        try {
            int totalCopies = 0;
            List<String> poolPaths = new ArrayList<>();
            for (Path poolRoot : poolRoots) {
                if (!Files.isDirectory(poolRoot)) {
                    throw new IllegalArgumentException("Provider pool path is not a directory: " + poolRoot);
                }
                totalCopies += copyLibraryArtifactsToPool(library, poolRoot);
                poolPaths.add(poolRoot.toString());
            }
            mapping.setReplicateStatus("available");
            mapping.setLastReplicatedAt(Instant.now());
            contentLibraryDatacenterRepository.save(mapping);
            taskOrchestrationService.completeTask(
                    job.getId(),
                    Map.of(
                            "libraryId", libraryId.toString(),
                            "datacenterId", datacenterId.toString(),
                            "storageClassName", storageClassName,
                            "poolCount", poolRoots.size(),
                            "fileCopies", totalCopies,
                            "poolPaths", poolPaths));
        } catch (Exception e) {
            log.error(
                    "Datacenter replication failed for library {} datacenter {}: {}",
                    libraryId,
                    datacenterId,
                    e.getMessage(),
                    e);
            mapping.setReplicateStatus("failed");
            contentLibraryDatacenterRepository.save(mapping);
            taskOrchestrationService.failTask(job.getId(), e.getMessage());
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Datacenter replication failed", e);
        }

        ContentReplicateResponse response = new ContentReplicateResponse();
        response.setLibraryId(libraryId);
        response.setDatacenterId(datacenterId);
        response.setTaskId(job.getId());
        response.setStatus(ContentReplicateResponse.StatusEnum.ACCEPTED);
        response.setMessage("Datacenter replicate completed");
        return response;
    }

    /**
     * Queues Proxmox API upload work to the orchestrator (no {@code host_path} on pools).
     */
    private ContentReplicateResponse enqueueProxmoxDatacenterReplicate(
            ContentLibraryEntity library,
            ContentLibraryDatacenterEntity mapping,
            UUID libraryId,
            UUID datacenterId,
            String storageClassName,
            UUID providerId,
            List<ProviderStorageEntity> scoped) {
        ContentStorageEntity storage = contentStorageRepository
                .findById(library.getContentStorageId())
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found for library: " + libraryId));
        if (contentStorageS3Uploader.isS3(storage)) {
            throw new IllegalArgumentException(
                    "Datacenter replication from S3-backed content storage is not supported in this release. "
                            + "Use local or NFS content storage for Proxmox API replication.");
        }

        Path artifactRoot = contentStoragePathResolver.artifactRootForStorage(storage).toAbsolutePath().normalize();

        List<ContentItemEntity> items = contentItemRepository.findByLibraryId(libraryId);
        for (ContentItemEntity item : items) {
            if ("available".equalsIgnoreCase(item.getContentStatus())) {
                contentLibraryProviderPathBuilder.applyProviderPaths(item);
                contentItemRepository.save(item);
            }
        }

        String storagePoolIds = scoped.stream()
                .map(ProviderStorageEntity::getExternalId)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(","));
        if (storagePoolIds.isBlank()) {
            throw new IllegalArgumentException(
                    "Matched Proxmox pools have no external storage ids; run storage discovery.");
        }

        TaskCreateRequest request = new TaskCreateRequest();
        request.setOperation(JobType.CONTENT_DATACENTER_REPLICATE);
        request.setEntityType(EntityType.PROVIDER);
        request.setEntityId(libraryId);
        request.setTimeoutSeconds(DEFAULT_TASK_TIMEOUT_SECONDS);
        request.setParameters(Map.of(
                "libraryId", libraryId.toString(),
                "datacenterId", datacenterId.toString(),
                "storageClassName", storageClassName,
                "providerId", providerId.toString()));
        request.setMetadata(Map.of(
                "kind", "CONTENT_DATACENTER_REPLICATE",
                "libraryId", libraryId.toString(),
                "datacenterId", datacenterId.toString(),
                "storageClassName", storageClassName,
                "providerId", providerId.toString()));

        JobEntity job = taskOrchestrationService.createTask(request);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("source", "core-services");
        metadata.put(ProviderQueueMetadataKeys.JOB_ID, job.getId().toString());
        metadata.put(
                ProviderQueueMetadataKeys.EXECUTION_TIMEOUT_SECONDS,
                String.valueOf(DEFAULT_TASK_TIMEOUT_SECONDS));
        metadata.put(ProviderQueueMetadataKeys.LIBRARY_ID, libraryId.toString());
        metadata.put(ProviderQueueMetadataKeys.DATACENTER_ID, datacenterId.toString());
        metadata.put(ProviderQueueMetadataKeys.ARTIFACT_ROOT, artifactRoot.toString());
        metadata.put(ProviderQueueMetadataKeys.STORAGE_POOL_IDS, storagePoolIds);

        Instant now = Instant.now();
        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.CONTENT_DATACENTER_REPLICATE)
                .entityType(com.onetattva.infron.api.model.EntityType.PROVIDER)
                .entityId(providerId)
                .payload(Map.of())
                .metadata(metadata)
                .source("core-services")
                .actorType("SYSTEM")
                .actorService("core-services")
                .createdAt(now)
                .correlationId(job.getId().toString())
                .build();

        UUID commandId = commandQueue.sendCommand(command);
        log.info(
                "Proxmox datacenter replicate enqueued: libraryId={}, datacenterId={}, jobId={}, commandId={}, pools={}",
                libraryId,
                datacenterId,
                job.getId(),
                commandId,
                storagePoolIds);
        log.debug(
                "Proxmox datacenter replicate command metadata: commandId={}, artifactRoot={}, jobId={}, "
                        + "executionTimeoutSec={}",
                commandId,
                artifactRoot,
                job.getId(),
                DEFAULT_TASK_TIMEOUT_SECONDS);

        mapping.setReplicateStatus("replicating");
        contentLibraryDatacenterRepository.save(mapping);

        ContentReplicateResponse response = new ContentReplicateResponse();
        response.setLibraryId(libraryId);
        response.setDatacenterId(datacenterId);
        response.setTaskId(job.getId());
        response.setStatus(ContentReplicateResponse.StatusEnum.ACCEPTED);
        response.setMessage("Datacenter replicate queued for Proxmox (orchestrator will upload via API)");
        return response;
    }

    private static String hostPathFrom(ProviderStorageEntity ps) {
        if (ps.getCapabilities() == null) {
            return null;
        }
        Object v = ps.getCapabilities().get(PROVIDER_STORAGE_HOST_PATH_CAP);
        return v != null ? v.toString().trim() : null;
    }

    /**
     * Copies files preserving {@code providerRelativePath} under {@code poolRoot} (same layout as under content-storage).
     */
    private int copyLibraryArtifactsToPool(ContentLibraryEntity library, Path poolRoot) throws IOException {
        UUID libraryId = library.getId();
        ContentStorageEntity storage = contentStorageRepository
                .findById(library.getContentStorageId())
                .orElseThrow(() -> new EntityNotFoundException("Content storage not found for library: " + libraryId));
        if (contentStorageS3Uploader.isS3(storage)) {
            throw new IllegalArgumentException(
                    "Datacenter replication from S3-backed content storage is not supported in this release. "
                            + "Use local or NFS content storage, or mount the artifact tree at the provider pool.");
        }

        Path artifactRoot = contentStoragePathResolver.artifactRootForStorage(storage).toAbsolutePath().normalize();
        List<ContentItemEntity> items = contentItemRepository.findByLibraryId(libraryId);
        int replicated = 0;
        for (ContentItemEntity item : items) {
            if (!"available".equalsIgnoreCase(item.getContentStatus())) {
                continue;
            }
            contentLibraryProviderPathBuilder.applyProviderPaths(item);
            String rel = item.getProviderRelativePath();
            if (rel == null || rel.isBlank()) {
                throw new IllegalStateException("Content item " + item.getId() + " has no provider relative path");
            }
            Path src = artifactRoot.resolve(rel).normalize();
            if (!src.startsWith(artifactRoot)) {
                throw new IllegalStateException("Refusing to read outside artifact root: " + src);
            }
            if (!Files.isRegularFile(src)) {
                throw new IllegalStateException(
                        "Missing artifact file for item " + item.getId() + " (expected at " + src + ")");
            }
            Path dst = poolRoot.resolve(rel).normalize();
            if (!dst.startsWith(poolRoot)) {
                throw new IllegalStateException("Refusing to write outside pool root: " + dst);
            }
            Files.createDirectories(dst.getParent());
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            replicated++;
        }
        return replicated;
    }
}
