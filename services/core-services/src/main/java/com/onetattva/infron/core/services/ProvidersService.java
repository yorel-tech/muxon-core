package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.api.enums.QueueStatus;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import com.onetattva.infron.db.repository.VmRepository;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.ProviderQueueCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing infrastructure providers.
 */
@Service
public class ProvidersService {

    private static final Logger logger = LoggerFactory.getLogger(ProvidersService.class);

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private VmRepository vmRepository;

    @Autowired
    private QueueEntryRepository queueEntryRepository;

    @Autowired
    private CommandQueue commandQueue;

    @Autowired
    private com.onetattva.infron.core.services.storage.ProviderStorageDiscoveryService storageDiscoveryService;
    private static final String META_CORRELATION_ID = "connectionTestCorrelationId";
    private static final String META_MESSAGE = "connectionTestMessage";
    private static final String META_LATENCY_MS = "connectionTestLatencyMs";

    /**
     * Create a new provider
     */
    public Provider createProvider(ProviderCreate request) {
        logger.info("Creating provider: {}", request.getName());

        // Validate provider name uniqueness
        if (providerRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), null)) {
            throw new IllegalArgumentException("Provider with name '" + request.getName() + "' already exists");
        }

        // Validate endpoint format based on provider type
        validateEndpointFormat(request.getType(), request.getEndpoint());

        // Validate credentials based on provider type
        validateCredentials(request.getType(), request.getCredentials());

        // Create provider entity
        // Let JPA/Hibernate generate the ID to avoid optimistic locking issues
        ProviderEntity entity = new ProviderEntity();
        entity.setName(request.getName());
        entity.setType(request.getType());
        entity.setEndpoint(request.getEndpoint());
        entity.setCredentials(request.getCredentials());
        entity.setMetadata(request.getMetadata() != null ? new HashMap<>(request.getMetadata()) : new HashMap<>());
        entity.setStatus(ProviderStatus.CONNECTING.name());

        // Save to database
        ProviderEntity savedEntity = providerRepository.save(entity);

        // Provider add path must validate connectivity before returning success.
        ProviderConnectionTestResult testResult = testProviderConnection(savedEntity.getId());
        if (!Boolean.TRUE.equals(testResult.getSuccess())) {
            providerRepository.delete(savedEntity);
            throw new IllegalArgumentException("Unable to add provider: " + testResult.getMessage());
        }

        // For Proxmox, capability discovery runs asynchronously in orchestrator.
        UUID providerId = savedEntity.getId();
        if (savedEntity.getType() == ProviderType.PROXMOX) {
            enqueueCapabilitiesDiscovery(providerId);
        }

        // Auto-discover storage for the provider
        try {
            UUID storageTaskId = storageDiscoveryService.enqueueStorageDiscovery(providerId);
            logger.info("Auto-enqueued storage discovery for provider {}: taskId={}", providerId, storageTaskId);
        } catch (Exception e) {
            logger.warn("Failed to enqueue storage discovery for provider {}: {}", providerId, e.getMessage());
            // Don't fail provider creation if storage discovery fails
        }

        savedEntity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found after creation: " + providerId));

        logger.info("Provider created successfully: {}", savedEntity.getId());
        return mapEntityToApi(savedEntity);
    }

    /**
     * Get a provider by ID
     */
    @Transactional(readOnly = true)
    public Provider getProvider(UUID providerId) {
        logger.debug("Getting provider: {}", providerId);
        return providerRepository.findById(providerId)
                .map(this::mapEntityToApi)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
    }

    /**
     * Get provider entity by ID.
     */
    @Transactional(readOnly = true)
    public ProviderEntity getProviderEntity(UUID providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
    }

    /**
     * List providers with pagination and filtering
     */
    @Transactional(readOnly = true)
    public ProviderList listProviders(Integer page, Integer perPage, ProviderType type, ProviderStatus status, String sort) {
        logger.debug("Listing providers: page={}, perPage={}, type={}, status={}", page, perPage, type, status);

        // Query providers
        List<ProviderEntity> allProviders;
        if (type != null && status != null) {
            allProviders = providerRepository.findByTypeAndStatus(type, status.name());
        } else if (type != null) {
            allProviders = providerRepository.findByType(type);
        } else if (status != null) {
            allProviders = providerRepository.findByStatus(status.name());
        } else {
            allProviders = providerRepository.findAll();
        }

        // Sort providers
        String sortField = sort != null ? sort : "createdAt";
        List<ProviderEntity> sortedProviders = allProviders.stream()
                .sorted((a, b) -> {
                    int cmp = 0;
                    if ("name".equals(sortField)) {
                        cmp = a.getName().compareToIgnoreCase(b.getName());
                    } else if ("type".equals(sortField)) {
                        cmp = a.getType().compareTo(b.getType());
                    } else if ("status".equals(sortField)) {
                        cmp = a.getStatus().compareTo(b.getStatus());
                    } else {
                        cmp = a.getCreatedAt().compareTo(b.getCreatedAt());
                    }
                    return cmp; // Default ascending, will reverse below
                })
                .collect(Collectors.toList());

        // Reverse for descending order
        java.util.Collections.reverse(sortedProviders);

        // Apply pagination
        int total = sortedProviders.size();
        int startIndex = (page - 1) * perPage;
        int endIndex = Math.min(startIndex + perPage, total);
        List<ProviderEntity> paginatedProviders = sortedProviders.subList(startIndex, endIndex);

        // Map to API models
        List<Provider> items = paginatedProviders.stream()
                .map(this::mapEntityToApi)
                .toList();

        ProviderList result = new ProviderList();
        result.setTotal(total);
        result.setPage(page);
        result.setPerPage(perPage);
        result.setItems(items);

        return result;
    }

    /**
     * Replace all provider configuration
     */
    @Transactional
    public Provider replaceProvider(UUID providerId, ProviderUpdate request) {
        logger.info("Replacing provider: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Check name uniqueness if name is being changed
        if (request.getName() != null && !request.getName().equals(entity.getName())) {
            if (providerRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), providerId)) {
                throw new IllegalArgumentException("Provider with name '" + request.getName() + "' already exists");
            }
        }

        // Validate endpoint format if being changed
        if (request.getEndpoint() != null) {
            validateEndpointFormat(entity.getType(), request.getEndpoint());
        }

        // Validate credentials if being changed
        if (request.getCredentials() != null) {
            validateCredentials(entity.getType(), request.getCredentials());
        }

        // Update entity
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getEndpoint() != null) {
            entity.setEndpoint(request.getEndpoint());
        }
        if (request.getCredentials() != null) {
            entity.setCredentials(request.getCredentials());
        }
        if (request.getMetadata() != null) {
            entity.setMetadata(request.getMetadata());
        }
        entity.setUpdatedAt(Instant.now());

        ProviderEntity savedEntity = providerRepository.save(entity);

        logger.info("Provider replaced successfully: {}", savedEntity.getId());
        return mapEntityToApi(savedEntity);
    }

    /**
     * Partially update provider
     */
    @Transactional
    public Provider updateProvider(UUID providerId, ProviderUpdate request) {
        logger.info("Updating provider: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Check name uniqueness if name is being changed
        if (request.getName() != null && !request.getName().equals(entity.getName())) {
            if (providerRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), providerId)) {
                throw new IllegalArgumentException("Provider with name '" + request.getName() + "' already exists");
            }
        }

        // Validate endpoint format if being changed
        if (request.getEndpoint() != null) {
            validateEndpointFormat(entity.getType(), request.getEndpoint());
        }

        // Validate credentials if being changed
        if (request.getCredentials() != null) {
            validateCredentials(entity.getType(), request.getCredentials());
        }

        // Update only provided fields
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getEndpoint() != null) {
            entity.setEndpoint(request.getEndpoint());
        }
        if (request.getCredentials() != null) {
            entity.setCredentials(request.getCredentials());
        }
        if (request.getMetadata() != null) {
            entity.setMetadata(request.getMetadata());
        }
        entity.setUpdatedAt(Instant.now());

        ProviderEntity savedEntity = providerRepository.save(entity);

        logger.info("Provider updated successfully: {}", savedEntity.getId());
        return mapEntityToApi(savedEntity);
    }

    /**
     * Delete a provider
     */
    @Transactional
    public void deleteProvider(UUID providerId) {
        logger.info("Deleting provider: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Check for active VMs
        long activeVmCount = vmRepository.countByProviderId(providerId);
        if (activeVmCount > 0) {
            throw new IllegalStateException("Cannot delete provider with " + activeVmCount + " active VMs");
        }

        // Check for datacenter references
        // Note: This would require a DatacenterRepository check
        // For now, we'll skip this check as it requires additional dependencies

        // Delete from database
        providerRepository.delete(entity);

        logger.info("Provider deleted successfully: {}", providerId);
    }

    /**
     * Test provider connection
     */
    public ProviderConnectionTestResult testProviderConnection(UUID providerId) {
        logger.info("Testing provider connection: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        String correlationId = UUID.randomUUID().toString();

        // Mark provider as CONNECTING and attach correlation metadata.
        entity.setStatus(ProviderStatus.CONNECTING.name());
        if (entity.getMetadata() == null) {
            entity.setMetadata(new HashMap<>());
        }
        entity.getMetadata().put(META_CORRELATION_ID, correlationId);
        entity.getMetadata().remove(META_MESSAGE);
        entity.getMetadata().remove(META_LATENCY_MS);
        entity.setUpdatedAt(Instant.now());
        providerRepository.save(entity);

        // Enqueue provider connection test command for the orchestrator.
        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.CONNECTION_TEST)
                .entityType(EntityType.PROVIDER)
                .entityId(providerId)
                .payload(Map.of())
                .metadata(Map.of("source", "api"))
                .source("core-services")
                .actorType("USER")
                .actorService("api")
                .createdAt(Instant.now())
                .correlationId(correlationId)
                .build();

        UUID commandId = commandQueue.sendCommand(command);
        logger.info("Enqueued provider connection test command: providerId={}, commandId={}, correlationId={}",
                providerId, commandId, correlationId);

        long deadline = System.currentTimeMillis() + 30_000L;
        while (System.currentTimeMillis() < deadline) {
            QueueEntryRepository.QueueStatusErrorProjection queueState = queueEntryRepository
                    .findStatusAndErrorById(commandId)
                    .orElse(null);
            QueueStatus queueStatus = queueState != null ? queueState.getStatus() : null;

            ProviderRepository.ProviderConnectionStateProjection current = providerRepository
                    .findConnectionStateById(providerId)
                    .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
            Map<String, String> currentMetadata = current.getMetadata() != null ? current.getMetadata() : Map.of();
            String currentCorrelationId = currentMetadata.get(META_CORRELATION_ID);
            if (queueStatus == QueueStatus.COMPLETED || queueStatus == QueueStatus.FAILED) {
                ProviderStatus status = ProviderStatus.valueOf(current.getStatus());
                if (status != ProviderStatus.CONNECTING) {
                    return mapConnectionTestResult(current, status);
                }
            }
            if (correlationId.equals(currentCorrelationId)) {
                ProviderStatus status = ProviderStatus.valueOf(current.getStatus());
                if (status != ProviderStatus.CONNECTING) {
                    return mapConnectionTestResult(current, status);
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // Timeout: mark as ERROR so callers get a deterministic failure.
        ProviderEntity timedOut = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        timedOut.setStatus(ProviderStatus.ERROR.name());
        String timeoutMessage = buildConnectionTimeoutMessage(providerId, commandId);
        logger.error("Provider connection test timed out: providerId={}, commandId={}, correlationId={}",
                providerId, commandId, correlationId);
        timedOut.getMetadata().put(META_MESSAGE, timeoutMessage);
        timedOut.getMetadata().put(META_LATENCY_MS, "0");
        timedOut.setUpdatedAt(Instant.now());
        providerRepository.save(timedOut);

        ProviderConnectionTestResult result = new ProviderConnectionTestResult();
        result.setSuccess(false);
        result.setStatus(ProviderStatus.ERROR);
        result.setMessage(timeoutMessage);
        result.setCapabilities(null);
        result.setLatencyMs(null);
        return result;
    }

    private String buildConnectionTimeoutMessage(UUID providerId, UUID commandId) {
        QueueEntryRepository.QueueStatusErrorProjection queueState = queueEntryRepository
                .findStatusAndErrorById(commandId)
                .orElse(null);
        QueueStatus status = queueState != null ? queueState.getStatus() : null;
        String errorMessage = queueState != null ? queueState.getErrorMessage() : null;

        StringBuilder message = new StringBuilder("Connection test timed out for provider ")
                .append(providerId)
                .append(" (commandId: ")
                .append(commandId)
                .append(")");
        if (status != null) {
            message.append(" (queue status: ").append(status).append(")");
        } else {
            message.append(" (queue entry not found)");
        }
        if (errorMessage != null && !errorMessage.isBlank()) {
            message.append(" - queue error: ").append(errorMessage);
        }
        return message.toString();
    }

    private void enqueueCapabilitiesDiscovery(UUID providerId) {
        CommandMessage command = CommandMessage.builder()
                .queueType(ProviderQueueCommands.CAPABILITIES_DISCOVERY)
                .entityType(EntityType.PROVIDER)
                .entityId(providerId)
                .payload(Map.of())
                .metadata(Map.of("source", "api"))
                .source("core-services")
                .actorType("USER")
                .actorService("api")
                .createdAt(Instant.now())
                .correlationId(UUID.randomUUID().toString())
                .build();

        commandQueue.sendCommand(command);
    }

    private ProviderConnectionTestResult mapConnectionTestResult(ProviderRepository.ProviderConnectionStateProjection entity, ProviderStatus status) {
        ProviderConnectionTestResult result = new ProviderConnectionTestResult();
        boolean success = status == ProviderStatus.ACTIVE;
        result.setSuccess(success);
        result.setStatus(status);

        Map<String, String> metadata = entity.getMetadata() != null ? entity.getMetadata() : Map.of();
        String message = metadata.get(META_MESSAGE);
        result.setMessage(message != null ? message : (success ? "Successfully connected to provider" : "Connection failed"));

        String latencyStr = metadata.get(META_LATENCY_MS);
        if (latencyStr != null) {
            try {
                result.setLatencyMs(Integer.parseInt(latencyStr));
            } catch (NumberFormatException ignored) {
                result.setLatencyMs(null);
            }
        } else {
            result.setLatencyMs(null);
        }

        result.setCapabilities(success ? mapEntityCapabilitiesToApi(entity.getCapabilities()) : null);
        return result;
    }

    /**
     * Get provider capabilities
     */
    @Transactional(readOnly = true)
    public ProviderCapabilities getProviderCapabilities(UUID providerId, Boolean refresh) {
        logger.debug("Getting provider capabilities: {}, refresh={}", providerId, refresh);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Return cached capabilities (live discovery is done by orchestrator)
        return mapEntityCapabilitiesToApi(entity.getCapabilities());
    }

    /**
     * Get provider metadata
     */
    @Transactional(readOnly = true)
    public Map<String, String> getProviderMetadata(UUID providerId) {
        logger.debug("Getting provider metadata: {}", providerId);
        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));
        return entity.getMetadata();
    }

    /**
     * Update provider metadata
     */
    @Transactional
    public Map<String, String> updateProviderMetadata(UUID providerId, Map<String, String> metadata) {
        logger.info("Updating provider metadata: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        entity.setMetadata(metadata);
        entity.setUpdatedAt(Instant.now());

        ProviderEntity savedEntity = providerRepository.save(entity);

        logger.info("Provider metadata updated successfully: {}", providerId);
        return savedEntity.getMetadata();
    }

    /**
     * Validate endpoint format based on provider type
     */
    private void validateEndpointFormat(ProviderType type, String endpoint) {
        switch (type) {
            case PROXMOX:
                if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
                    throw new IllegalArgumentException("Proxmox endpoint must be HTTP/HTTPS URL");
                }
                break;
            case LIBVIRT:
                // Libvirt can use SSH connection string or simple hostname
                if (!endpoint.startsWith("ssh://") && !endpoint.startsWith("libvirt://")) {
                    throw new IllegalArgumentException("Libvirt endpoint must be SSH connection string or libvirt:// URL");
                }
                break;
        }
    }

    /**
     * Validate credentials based on provider type
     */
    private void validateCredentials(ProviderType type, Map<String, String> credentials) {
        if (credentials == null || credentials.isEmpty()) {
            throw new IllegalArgumentException("Credentials are required");
        }

        switch (type) {
            case PROXMOX:
                if (!credentials.containsKey("username") || !credentials.containsKey("password")) {
                    throw new IllegalArgumentException("Proxmox requires username and password");
                }
                break;
            case LIBVIRT:
                if (!credentials.containsKey("sshPrivateKey")) {
                    throw new IllegalArgumentException("Libvirt requires sshPrivateKey");
                }
                break;
        }
    }

    /**
     * Map ProviderEntity to Provider API model
     */
    private Provider mapEntityToApi(ProviderEntity entity) {
        Provider api = new Provider();
        api.setId(entity.getId());
        api.setName(entity.getName());
        api.setType(entity.getType());
        api.setEndpoint(entity.getEndpoint());
        api.setStatus(ProviderStatus.valueOf(entity.getStatus()));
        api.setCapabilities(mapEntityCapabilitiesToApi(entity.getCapabilities()));
        api.setMetadata(entity.getMetadata());
        api.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        api.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        return api;
    }

    /**
     * Map entity capabilities to API model
     */
    private ProviderCapabilities mapEntityCapabilitiesToApi(Map<String, String> entityCapabilities) {
        if (entityCapabilities == null) {
            entityCapabilities = Map.of();
        }

        Integer maxMemoryGb = parseInteger(entityCapabilities.get("maxMemoryGb"));
        if (maxMemoryGb == null) {
            Integer maxMemoryMb = parseInteger(entityCapabilities.get("maxMemoryMb"));
            if (maxMemoryMb != null) {
                maxMemoryGb = maxMemoryMb / 1024;
            }
        }

        return ProviderCapabilities.builder()
                .supportedCpuTypes(parseCsvList(entityCapabilities.get("supportedCpuTypes")))
                .supportedStorageClasses(parseCsvList(entityCapabilities.get("supportedStorageClasses")))
                .supportedNetworkTypes(parseCsvList(entityCapabilities.get("supportedNetworkTypes")))
                .supportedOsTypes(parseCsvList(entityCapabilities.get("supportedOsTypes")))
                .resourceLimits(ResourceLimits.builder()
                        .maxCpus(parseInteger(entityCapabilities.get("maxCpus")))
                        .maxMemoryGb(maxMemoryGb)
                        .maxStorageGb(parseInteger(entityCapabilities.get("maxStorageGb")))
                        .maxVms(parseInteger(entityCapabilities.get("maxVms")))
                        .build())
                .features(new java.util.HashMap<>(entityCapabilities))
                .build();
    }

    private List<String> parseCsvList(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }

        String[] splitValues = rawValue.split(",");
        List<String> parsed = new ArrayList<>(splitValues.length);
        for (String value : splitValues) {
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        return parsed;
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

}
