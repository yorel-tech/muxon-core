package com.krito.muxon.controllers;

import com.krito.muxon.api.ProviderStorageApi;
import com.krito.muxon.api.ProvidersApi;
import com.krito.muxon.api.model.*;
import com.krito.muxon.auth.Permission;
import com.krito.muxon.auth.ResourceAction;
import com.krito.muxon.services.ProviderInventorySyncService;
import com.krito.muxon.services.ProvidersService;
import com.krito.muxon.services.storage.ProviderStorageApiConverter;
import com.krito.muxon.services.storage.ProviderStorageDiscoveryService;
import com.krito.muxon.services.storage.ProviderStorageMappingService;
import com.krito.muxon.db.model.ProviderStorageEntity;
import com.krito.muxon.db.repository.ProviderStorageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * REST controller for managing infrastructure providers.
 */
@RestController
public class ProvidersController implements ProvidersApi, ProviderStorageApi {

    private final ProvidersService providersService;
    private final ProviderStorageRepository providerStorageRepository;
    private final ProviderStorageDiscoveryService providerStorageDiscoveryService;
    private final ProviderStorageMappingService providerStorageMappingService;
    private final ProviderStorageApiConverter providerStorageApiConverter;
    private final ProviderInventorySyncService providerInventorySyncService;
    private final ObjectMapper objectMapper;

    public ProvidersController(
            final ProvidersService providersService,
            final ProviderStorageRepository providerStorageRepository,
            final ProviderStorageDiscoveryService providerStorageDiscoveryService,
            final ProviderStorageMappingService providerStorageMappingService,
            final ProviderStorageApiConverter providerStorageApiConverter,
            final ProviderInventorySyncService providerInventorySyncService,
            final ObjectMapper objectMapper) {
        this.providersService = providersService;
        this.providerStorageRepository = providerStorageRepository;
        this.providerStorageDiscoveryService = providerStorageDiscoveryService;
        this.providerStorageMappingService = providerStorageMappingService;
        this.providerStorageApiConverter = providerStorageApiConverter;
        this.providerInventorySyncService = providerInventorySyncService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<ProviderList> listProviders(Integer page, Integer perPage, ProviderType type, ProviderStatus status
    ) {
        ProviderList providerList = providersService.listProviders(page, perPage, type, status, null);
        return ResponseEntity.ok(providerList);
    }

    @Override
    public ResponseEntity<Provider> createProvider(ProviderCreate providerCreate) {
        Provider provider = providersService.createProvider(providerCreate);
        return ResponseEntity.status(201).body(provider);
    }

    @Override
    @ResourceAction(rel = "self", method = RequestMethod.GET, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    public ResponseEntity<Provider> getProvider(UUID providerId) {
        Provider provider = providersService.getProvider(providerId);
        return ResponseEntity.ok(provider);
    }

    @Override
    @ResourceAction(rel = "edit", title = "Replace provider", method = RequestMethod.PUT, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    public ResponseEntity<Provider> replaceProvider(UUID providerId, ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.replaceProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    @ResourceAction(rel = "update", title = "Update provider", method = RequestMethod.PATCH, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    public ResponseEntity<Provider> updateProvider(
            UUID providerId,
            ProviderUpdate providerUpdate
    ) {
        Provider provider = providersService.updateProvider(providerId, providerUpdate);
        return ResponseEntity.ok(provider);
    }

    @Override
    @ResourceAction(rel = "delete", method = RequestMethod.DELETE, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_MANAGE)
    public ResponseEntity<Void> deleteProvider(UUID providerId) {
        providersService.deleteProvider(providerId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @ResourceAction(rel = "test", title = "Test connection", method = RequestMethod.POST, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    public ResponseEntity<ProviderConnectionTestResult> testProviderConnection(UUID providerId) {
        ProviderConnectionTestResult result = providersService.testProviderConnection(providerId);
        return ResponseEntity.ok(result);
    }

    @Override
    @ResourceAction(rel = "capabilities", title = "Get capabilities", method = RequestMethod.GET, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    public ResponseEntity<ProviderCapabilities> getProviderCapabilities(UUID providerId, Boolean refresh
    ) {
        ProviderCapabilities capabilities = providersService.getProviderCapabilities(providerId, refresh);
        return ResponseEntity.ok(capabilities);
    }

    @ResourceAction(rel = "metadata", method = RequestMethod.GET, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    @org.springframework.web.bind.annotation.RequestMapping(
            method = RequestMethod.GET,
            value = "/api/v1/providers/{providerId}/metadata",
            produces = { "application/json" }
    )
    public ResponseEntity<Map<String, String>> getProviderMetadata(UUID providerId) {
        Map<String, String> metadata = providersService.getProviderMetadata(providerId);
        return ResponseEntity.ok(metadata != null ? metadata : Map.of());
    }

    @ResourceAction(rel = "metadata", title = "Update provider metadata", method = RequestMethod.PUT, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    @org.springframework.web.bind.annotation.RequestMapping(
            method = RequestMethod.PUT,
            value = "/api/v1/providers/{providerId}/metadata",
            produces = { "application/json" },
            consumes = { "application/json" }
    )
    public ResponseEntity<Map<String, String>> updateProviderMetadata(UUID providerId, InputStream body) {
        Map<String, String> requestBody;
        try {
            requestBody = objectMapper.readValue(body, new TypeReference<>() { });
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Invalid metadata JSON: " + e.getOriginalMessage());
        }
        Map<String, String> updated = providersService.updateProviderMetadata(providerId, requestBody);
        return ResponseEntity.ok(updated != null ? updated : Map.of());
    }

    @Override
    public ResponseEntity<ProviderStorageList> listProviderStorage(
            Integer page,
            Integer perPage,
            UUID providerId,
            UUID datacenterId,
            String storageClass) {
        providerStorageMappingService.requireStorageClassExists(storageClass);
        ProviderStorageMappingService.MappingBatchContext ctx = providerStorageMappingService.loadBatchContext();
        Stream<ProviderStorageEntity> stream = providerStorageRepository.findAll().stream();
        if (providerId != null) {
            stream = stream.filter(e -> providerId.equals(e.getProviderId()));
        }
        if (datacenterId != null) {
            stream = stream.filter(e -> datacenterId.equals(e.getDatacenterId()));
        }
        if (storageClass != null && !storageClass.isBlank()) {
            String sc = storageClass.trim();
            stream = stream.filter(e -> providerStorageMappingService.isMappedToStorageClass(e, sc, ctx));
        }
        List<ProviderStorageEntity> filtered = stream.collect(Collectors.toList());
        return ResponseEntity.ok(providerStorageApiConverter.toPagedList(filtered, page, perPage, ctx));
    }

    @Override
    public ResponseEntity<ProviderStorage> getProviderStorage(UUID id) {
        ProviderStorageEntity entity = providerStorageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider storage not found: " + id));
        ProviderStorageMappingService.MappingBatchContext ctx = providerStorageMappingService.loadBatchContext();
        return ResponseEntity.ok(providerStorageApiConverter.toApi(entity, ctx));
    }

    @Override
    @ResourceAction(rel = "storage", title = "List provider storage", method = RequestMethod.GET, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    public ResponseEntity<ProviderStorageList> listProviderStorageByProvider(
            UUID providerId, Integer page, Integer perPage, String storageClass) {
        providersService.getProvider(providerId);
        providerStorageMappingService.requireStorageClassExists(storageClass);
        ProviderStorageMappingService.MappingBatchContext ctx = providerStorageMappingService.loadBatchContext();
        Stream<ProviderStorageEntity> stream =
                providerStorageDiscoveryService.getProviderStorage(providerId).stream();
        if (storageClass != null && !storageClass.isBlank()) {
            String sc = storageClass.trim();
            stream = stream.filter(e -> providerStorageMappingService.isMappedToStorageClass(e, sc, ctx));
        }
        List<ProviderStorageEntity> filtered = stream.collect(Collectors.toList());
        return ResponseEntity.ok(providerStorageApiConverter.toPagedList(filtered, page, perPage, ctx));
    }

    @Override
    @ResourceAction(rel = "sync-storage", title = "Sync provider storage", method = RequestMethod.POST, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    public ResponseEntity<StorageSyncResponse> syncProviderStorage(UUID providerId) {
        providersService.getProvider(providerId);
        try {
            UUID taskId = providerStorageDiscoveryService.enqueueStorageDiscovery(providerId);
            StorageSyncResponse body = StorageSyncResponse.builder()
                    .providerId(providerId)
                    .taskId(taskId)
                    .status(StorageSyncResponse.StatusEnum.ACCEPTED)
                    .message("Storage discovery enqueued; poll GET /api/v1/tasks/" + taskId)
                    .build();
            return ResponseEntity.status(202).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    @ResourceAction(rel = "sync", title = "Sync provider inventory", method = RequestMethod.POST, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    public ResponseEntity<StorageSyncResponse> syncProvider(UUID providerId) {
        providersService.getProvider(providerId);
        try {
            UUID taskId = providerInventorySyncService.enqueueInventorySync(providerId);
            StorageSyncResponse body = StorageSyncResponse.builder()
                    .providerId(providerId)
                    .taskId(taskId)
                    .status(StorageSyncResponse.StatusEnum.ACCEPTED)
                    .message("Provider inventory sync enqueued; poll GET /api/v1/tasks/" + taskId)
                    .build();
            return ResponseEntity.status(202).body(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}
