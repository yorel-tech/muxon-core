package com.onetattva.infron.core.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.providers.VmProviderRegistry;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.VmRepository;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private VmProviderRegistry vmProviderRegistry;

    @Autowired
    private VmRepository vmRepository;

    /**
     * Create a new provider
     */
    @Transactional
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
        entity.setMetadata(request.getMetadata());
        entity.setStatus(ProviderStatus.CONNECTING.name());

        // Save to database
        ProviderEntity savedEntity = providerRepository.save(entity);

        // Discover and store capabilities
        try {
            Map<String, String> discoveredCapabilities = discoverCapabilities(savedEntity);
            savedEntity.setCapabilities(discoveredCapabilities);
            savedEntity = providerRepository.save(savedEntity);
            logger.info("Discovered capabilities for provider {}: {}", savedEntity.getId(), discoveredCapabilities);
        } catch (Exception e) {
            logger.warn("Failed to discover capabilities for provider {}: {}", savedEntity.getId(), e.getMessage());
            // Continue with provider creation even if capability discovery fails
        }

        // Register with VmProviderRegistry
        vmProviderRegistry.registerProvider(new VmProviderAdapter(savedEntity));

        // Update status to ACTIVE after successful registration
        savedEntity.setStatus(ProviderStatus.ACTIVE.name());
        providerRepository.save(savedEntity);

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

        // Unregister from VmProviderRegistry
        vmProviderRegistry.unregisterProvider(entity.getId().toString());

        // Delete from database
        providerRepository.delete(entity);

        logger.info("Provider deleted successfully: {}", providerId);
    }

    /**
     * Test provider connection
     */
    @Transactional
    public ProviderConnectionTestResult testProviderConnection(UUID providerId) {
        logger.info("Testing provider connection: {}", providerId);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        long startTime = System.currentTimeMillis();

        try {
            // Discover fresh capabilities during connection test
            Map<String, String> discoveredCapabilities = discoverCapabilities(entity);
            entity.setCapabilities(discoveredCapabilities);
            providerRepository.save(entity);

            long latency = System.currentTimeMillis() - startTime;

            ProviderConnectionTestResult result = new ProviderConnectionTestResult();
            result.setSuccess(true);
            result.setStatus(ProviderStatus.ACTIVE);
            result.setMessage("Successfully connected to provider");
            result.setCapabilities(mapEntityCapabilitiesToApi(entity.getCapabilities()));
            result.setLatencyMs((int) latency);

            // Update provider status
            entity.setStatus(ProviderStatus.ACTIVE.name());
            providerRepository.save(entity);

            logger.info("Provider connection test successful: {}", providerId);
            return result;

        } catch (Exception e) {
            logger.error("Provider connection test failed: {}", providerId, e);

            // Update provider status to ERROR
            entity.setStatus(ProviderStatus.ERROR.name());
            providerRepository.save(entity);

            ProviderConnectionTestResult result = new ProviderConnectionTestResult();
            result.setSuccess(false);
            result.setStatus(ProviderStatus.ERROR);
            result.setMessage("Connection failed: " + e.getMessage());
            result.setCapabilities(null);
            result.setLatencyMs(null);

            return result;
        }
    }

    /**
     * Get provider capabilities
     */
    @Transactional(readOnly = true)
    public ProviderCapabilities getProviderCapabilities(UUID providerId, Boolean refresh) {
        logger.debug("Getting provider capabilities: {}, refresh={}", providerId, refresh);

        ProviderEntity entity = providerRepository.findById(providerId)
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + providerId));

        // Return cached capabilities unless refresh is requested
        if (refresh != null && !refresh) {
            return mapEntityCapabilitiesToApi(entity.getCapabilities());
        }

        // Fetch live capabilities from provider
        // Note: This would use VmProviderRegistry to get live capabilities
        // For now, return cached capabilities
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
            case KUBERNETES:
                if (!endpoint.startsWith("https://") && !endpoint.startsWith("http://")) {
                    throw new IllegalArgumentException("Kubernetes endpoint must be HTTP/HTTPS URL");
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
            case KUBERNETES:
                if (!credentials.containsKey("kubeconfig") && !credentials.containsKey("token")) {
                    throw new IllegalArgumentException("Kubernetes requires kubeconfig or token");
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
        // Parse JSONB capabilities into typed object
        // This is a simplified implementation
        // Real implementation would properly deserialize the JSONB structure
        return ProviderCapabilities.builder()
                .supportedCpuTypes(List.of("kvm64", "host"))
                .supportedStorageClasses(List.of("local", "nfs"))
                .supportedNetworkTypes(List.of("bridge", "ovs"))
                .supportedOsTypes(List.of("linux", "windows"))
                .resourceLimits(ResourceLimits.builder()
                        .maxCpus(entityCapabilities != null && entityCapabilities.containsKey("maxCpus")
                                ? Integer.parseInt(entityCapabilities.get("maxCpus")) : 1000)
                        .maxMemoryGb(entityCapabilities != null && entityCapabilities.containsKey("maxMemoryGb")
                                ? Integer.parseInt(entityCapabilities.get("maxMemoryGb")) : 409600)
                        .maxStorageGb(entityCapabilities != null && entityCapabilities.containsKey("maxStorageGb")
                                ? Integer.parseInt(entityCapabilities.get("maxStorageGb")) : 20000)
                        .maxVms(entityCapabilities != null && entityCapabilities.containsKey("maxVms")
                                ? Integer.parseInt(entityCapabilities.get("maxVms")) : 500)
                        .build())
                .features(entityCapabilities != null ? new java.util.HashMap<>(entityCapabilities) : Map.of())
                .build();
    }

    /**
     * Discover capabilities from the actual provider implementation
     */
    private Map<String, String> discoverCapabilities(ProviderEntity entity) {
        logger.debug("Discovering capabilities for provider: {}", entity.getId());

        try {
            // Try to get provider from registry
            String providerId = entity.getId().toString();
            java.util.Optional<com.onetattva.infron.core.providers.VmProvider> providerOpt = vmProviderRegistry.getProvider(providerId);

            if (providerOpt.isEmpty()) {
                logger.warn("Provider not found in registry: {}", providerId);
                return getDefaultCapabilities();
            }

            // Get capabilities from provider
            com.onetattva.infron.core.providers.ProviderCapabilities caps =
                providerOpt.get().getCapabilities().join();

            // Convert to Map<String, String> for storage
            Map<String, String> capabilitiesMap = new java.util.HashMap<>();

            // Store CPU types as comma-separated string
            if (caps.supportedCpuTypes() != null && !caps.supportedCpuTypes().isEmpty()) {
                capabilitiesMap.put("supportedCpuTypes", String.join(",", caps.supportedCpuTypes()));
            }

            // Store storage classes as comma-separated string
            if (caps.supportedStorageClasses() != null && !caps.supportedStorageClasses().isEmpty()) {
                capabilitiesMap.put("supportedStorageClasses", String.join(",", caps.supportedStorageClasses()));
            }

            // Store network types as comma-separated string
            if (caps.supportedNetworkTypes() != null && !caps.supportedNetworkTypes().isEmpty()) {
                capabilitiesMap.put("supportedNetworkTypes", String.join(",", caps.supportedNetworkTypes()));
            }

            // Store OS types as comma-separated string
            if (caps.supportedOsTypes() != null && !caps.supportedOsTypes().isEmpty()) {
                capabilitiesMap.put("supportedOsTypes", String.join(",", caps.supportedOsTypes()));
            }

            // Store resource limits
            if (caps.resourceLimits() != null) {
                com.onetattva.infron.core.providers.ResourceLimits limits = caps.resourceLimits();
                capabilitiesMap.put("maxCpus", String.valueOf(limits.maxCpuCores()));
                capabilitiesMap.put("maxMemoryMb", String.valueOf(limits.maxMemoryMb()));
                capabilitiesMap.put("maxStorageGb", String.valueOf(limits.maxStorageGb()));
                capabilitiesMap.put("maxVms", String.valueOf(limits.maxVms()));
            }

            // Store features
            if (caps.features() != null && !caps.features().isEmpty()) {
                caps.features().forEach((key, value) -> {
                    capabilitiesMap.put("feature_" + key, String.valueOf(value));
                });
            }

            logger.debug("Discovered capabilities for provider {}: {}", entity.getId(), capabilitiesMap);
            return capabilitiesMap;

        } catch (Exception e) {
            logger.error("Failed to discover capabilities for provider {}: {}", entity.getId(), e.getMessage(), e);
            return getDefaultCapabilities();
        }
    }

    /**
     * Get default capabilities for providers that don't support discovery
     */
    private Map<String, String> getDefaultCapabilities() {
        Map<String, String> defaults = new java.util.HashMap<>();
        defaults.put("supportedCpuTypes", "kvm64,host");
        defaults.put("supportedStorageClasses", "local,nfs");
        defaults.put("supportedNetworkTypes", "bridge,ovs");
        defaults.put("supportedOsTypes", "linux,windows");
        defaults.put("maxCpus", "1000");
        defaults.put("maxMemoryMb", "409600");
        defaults.put("maxStorageGb", "20000");
        defaults.put("maxVms", "500");
        return defaults;
    }

    /**
     * Adapter class to integrate ProviderEntity with VmProvider SPI
     */
    private static class VmProviderAdapter implements com.onetattva.infron.core.providers.VmProvider {
        private final ProviderEntity entity;

        VmProviderAdapter(ProviderEntity entity) {
            this.entity = entity;
        }

        @Override
        public String id() {
            return entity.getId().toString();
        }

        @Override
        public String description() {
            return entity.getName() + " (" + entity.getType() + ")";
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.ProviderCapabilities> getCapabilities() {
            // Return capabilities from entity
            return java.util.concurrent.CompletableFuture.completedFuture(
                mapEntityCapabilitiesToSpi(entity.getCapabilities())
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmCreationResult> createVm(com.onetattva.infron.core.providers.VmCreationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM creation not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmDeletionResult> deleteVm(com.onetattva.infron.core.providers.VmDeletionRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM deletion not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmOperationResult> startVm(com.onetattva.infron.core.providers.VmOperationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM start not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmOperationResult> stopVm(com.onetattva.infron.core.providers.VmOperationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM stop not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmOperationResult> restartVm(com.onetattva.infron.core.providers.VmOperationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM restart not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmOperationResult> suspendVm(com.onetattva.infron.core.providers.VmOperationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM suspend not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.VmOperationResult> resumeVm(com.onetattva.infron.core.providers.VmOperationRequest request) {
            return java.util.concurrent.CompletableFuture.failedFuture(
                new UnsupportedOperationException("VM resume not implemented for provider adapter")
            );
        }

        @Override
        public java.util.concurrent.CompletableFuture<java.util.Optional<com.onetattva.infron.core.providers.VmInfo>> getVmInfo(String externalVmId) {
            return java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.empty());
        }

        @Override
        public java.util.concurrent.CompletableFuture<java.util.List<com.onetattva.infron.core.providers.VmInfo>> listVms(com.onetattva.infron.core.providers.VmListRequest request) {
            return java.util.concurrent.CompletableFuture.completedFuture(java.util.List.of());
        }

        @Override
        public java.util.concurrent.CompletableFuture<com.onetattva.infron.core.providers.ValidationResult> validateVmSpec(String spec) {
            return java.util.concurrent.CompletableFuture.completedFuture(
                new com.onetattva.infron.core.providers.ValidationResult(true, java.util.List.of("Validation not implemented for provider adapter"))
            );
        }

        /**
         * Map entity capabilities to SPI capabilities
         */
        private com.onetattva.infron.core.providers.ProviderCapabilities mapEntityCapabilitiesToSpi(Map<String, String> entityCapabilities) {
            if (entityCapabilities == null) {
                return new com.onetattva.infron.core.providers.ProviderCapabilities(
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    new com.onetattva.infron.core.providers.ResourceLimits(0, 0, 0, 0),
                    Map.of()
                );
            }

            // Parse resource limits from JSONB
            int maxCpuCores = entityCapabilities.containsKey("maxCpuCores")
                ? Integer.parseInt(entityCapabilities.get("maxCpuCores")) : 0;
            int maxMemoryMb = entityCapabilities.containsKey("maxMemoryMb")
                ? Integer.parseInt(entityCapabilities.get("maxMemoryMb")) : 0;
            int maxStorageGb = entityCapabilities.containsKey("maxStorageGb")
                ? Integer.parseInt(entityCapabilities.get("maxStorageGb")) : 0;
            int maxVms = entityCapabilities.containsKey("maxVms")
                ? Integer.parseInt(entityCapabilities.get("maxVms")) : 0;

            return new com.onetattva.infron.core.providers.ProviderCapabilities(
                    List.of("kvm64", "host"),
                    List.of("local", "nfs"),
                    List.of("bridge", "ovs"),
                    List.of("linux", "windows"),
                    new com.onetattva.infron.core.providers.ResourceLimits(maxCpuCores, maxMemoryMb, maxStorageGb, maxVms),
                    new java.util.HashMap<>(entityCapabilities)
            );
        }
    }
}
