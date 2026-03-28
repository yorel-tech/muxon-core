package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.ProviderStorageApi;
import com.onetattva.infron.api.ProvidersApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.ResourceAction;
import com.onetattva.infron.core.services.ProvidersService;
import com.onetattva.infron.core.services.storage.ProviderStorageDiscoveryService;
import com.onetattva.infron.db.model.ProviderStorageEntity;
import com.onetattva.infron.db.repository.ProviderStorageRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final ObjectMapper objectMapper;

    public ProvidersController(
            final ProvidersService providersService,
            final ProviderStorageRepository providerStorageRepository,
            final ProviderStorageDiscoveryService providerStorageDiscoveryService,
            final ObjectMapper objectMapper) {
        this.providersService = providersService;
        this.providerStorageRepository = providerStorageRepository;
        this.providerStorageDiscoveryService = providerStorageDiscoveryService;
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
            Integer page, Integer perPage, UUID providerId, UUID datacenterId) {
        Stream<ProviderStorageEntity> stream = providerStorageRepository.findAll().stream();
        if (providerId != null) {
            stream = stream.filter(e -> providerId.equals(e.getProviderId()));
        }
        if (datacenterId != null) {
            stream = stream.filter(e -> datacenterId.equals(e.getDatacenterId()));
        }
        List<ProviderStorageEntity> filtered = stream.collect(Collectors.toList());
        int total = filtered.size();
        int p = page != null ? page : 1;
        int pp = perPage != null ? perPage : 20;
        int from = Math.max(0, (p - 1) * pp);
        int to = Math.min(from + pp, total);
        List<ProviderStorage> items = from < total
                ? filtered.subList(from, to).stream().map(this::toProviderStorage).toList()
                : List.of();
        ProviderStorageList list = new ProviderStorageList();
        list.setTotal(total);
        list.setPage(p);
        list.setPerPage(pp);
        list.setItems(items);
        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<ProviderStorage> getProviderStorage(UUID id) {
        ProviderStorageEntity entity = providerStorageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Provider storage not found: " + id));
        return ResponseEntity.ok(toProviderStorage(entity));
    }

    @Override
    @ResourceAction(rel = "storage", title = "List provider storage", method = RequestMethod.GET, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_READ)
    public ResponseEntity<ProviderStorageList> listProviderStorageByProvider(
            UUID providerId, Integer page, Integer perPage) {
        providersService.getProvider(providerId);
        List<ProviderStorageEntity> all = providerStorageDiscoveryService.getProviderStorage(providerId);
        int total = all.size();
        int p = page != null ? page : 1;
        int pp = perPage != null ? perPage : 20;
        int from = Math.max(0, (p - 1) * pp);
        int to = Math.min(from + pp, total);
        List<ProviderStorage> items = from < total
                ? all.subList(from, to).stream().map(this::toProviderStorage).toList()
                : List.of();
        ProviderStorageList list = new ProviderStorageList();
        list.setTotal(total);
        list.setPage(p);
        list.setPerPage(pp);
        list.setItems(items);
        return ResponseEntity.ok(list);
    }

    @Override
    @ResourceAction(rel = "sync-storage", title = "Sync provider storage", method = RequestMethod.POST, resourceType = Provider.class, idParam = "providerId", permission = Permission.PROVIDER_EDIT)
    public ResponseEntity<StorageSyncResponse> syncProviderStorage(UUID providerId) {
        int count = providerStorageDiscoveryService.discoverAndSyncStorage(providerId);
        StorageSyncResponse body = StorageSyncResponse.builder()
                .providerId(providerId)
                .status(StorageSyncResponse.StatusEnum.COMPLETED)
                .discoveredCount(count)
                .message("Synced " + count + " storage entries")
                .build();
        return ResponseEntity.status(202).body(body);
    }

    private ProviderStorage toProviderStorage(ProviderStorageEntity entity) {
        ProviderType providerType = ProviderType.valueOf(entity.getProviderType().toUpperCase(Locale.ROOT));
        ProviderStorage api = new ProviderStorage(
                entity.getId(),
                entity.getProviderId(),
                providerType,
                entity.getExternalId(),
                entity.getStorageType(),
                mapStorageCapabilities(entity.getCapabilities()),
                mapStorageMetrics(entity.getMetrics())
        );
        api.setName(entity.getName());
        api.setDatacenterId(entity.getDatacenterId());
        api.setNodeId(entity.getNodeId());
        api.setEnabled(entity.getEnabled() != null ? entity.getEnabled() : true);
        if (entity.getSyncedAt() != null) {
            api.setSyncedAt(entity.getSyncedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getCreatedAt() != null) {
            api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
        }
        api.setMappedStorageClasses(new ArrayList<>());
        return api;
    }

    private static ProviderStorageCapabilities mapStorageCapabilities(Map<String, Object> raw) {
        Map<String, Object> m = raw != null ? raw : Map.of();
        ProviderStorageCapabilities caps = new ProviderStorageCapabilities();
        Object perf = m.get("performance");
        if (perf instanceof String s) {
            try {
                caps.setPerformance(ProviderStorageCapabilities.PerformanceEnum.fromValue(s));
            } catch (IllegalArgumentException ignored) {
                caps.putAdditionalProperty("performance", s);
            }
        } else if (perf != null) {
            caps.putAdditionalProperty("performance", perf);
        }
        Object media = m.get("media");
        if (media instanceof String s) {
            try {
                caps.setMedia(ProviderStorageCapabilities.MediaEnum.fromValue(s));
            } catch (IllegalArgumentException ignored) {
                caps.putAdditionalProperty("media", s);
            }
        } else if (media != null) {
            caps.putAdditionalProperty("media", media);
        }
        if (m.get("shared") instanceof Boolean b) {
            caps.setShared(b);
        } else if (m.containsKey("shared")) {
            caps.putAdditionalProperty("shared", m.get("shared"));
        }
        Object redundancy = m.get("redundancy");
        if (redundancy instanceof String s) {
            try {
                caps.setRedundancy(ProviderStorageCapabilities.RedundancyEnum.fromValue(s));
            } catch (IllegalArgumentException ignored) {
                caps.putAdditionalProperty("redundancy", s);
            }
        } else if (redundancy != null) {
            caps.putAdditionalProperty("redundancy", redundancy);
        }
        for (Map.Entry<String, Object> e : m.entrySet()) {
            String k = e.getKey();
            if (k.equals("performance") || k.equals("media") || k.equals("shared") || k.equals("redundancy")) {
                continue;
            }
            caps.putAdditionalProperty(k, e.getValue());
        }
        return caps;
    }

    private static ProviderStorageMetrics mapStorageMetrics(Map<String, Object> raw) {
        Map<String, Object> m = raw != null ? raw : Map.of();
        ProviderStorageMetrics metrics = new ProviderStorageMetrics();
        metrics.setTotalGb(intObject(m.get("total_gb")));
        metrics.setFreeGb(intObject(m.get("free_gb")));
        metrics.setUsedGb(intObject(m.get("used_gb")));
        metrics.setEstimatedIops(intObject(m.get("estimated_iops")));
        Object lat = m.get("latency_ms");
        if (lat instanceof Number n) {
            metrics.setLatencyMs(n.doubleValue());
        }
        for (Map.Entry<String, Object> e : m.entrySet()) {
            String k = e.getKey();
            if (k.equals("total_gb") || k.equals("free_gb") || k.equals("used_gb")
                    || k.equals("estimated_iops") || k.equals("latency_ms")) {
                continue;
            }
            metrics.putAdditionalProperty(k, e.getValue());
        }
        return metrics;
    }

    private static Integer intObject(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return n.intValue();
        }
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
