package com.krito.muxon.services.storage;

import com.krito.muxon.api.model.ProviderType;
import com.krito.muxon.api.model.StorageCapabilities;
import com.krito.muxon.api.model.StorageClass;
import com.krito.muxon.api.model.StorageClassCreate;
import com.krito.muxon.api.model.StorageClassList;
import com.krito.muxon.api.model.StorageClassOverride;
import com.krito.muxon.api.model.StorageClassOverrides;
import com.krito.muxon.api.model.StorageClassUpdate;
import com.krito.muxon.api.model.StorageConstraints;
import com.krito.muxon.common.EntityNotFoundException;
import com.krito.muxon.db.model.StorageClassEntity;
import com.krito.muxon.db.model.StorageOverrideEntity;
import com.krito.muxon.db.repository.StorageClassRepository;
import com.krito.muxon.db.repository.StorageOverrideRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StorageClassesService {

    private static final String FEATURES_METADATA_KEY = "apiMetadata";

    private final StorageClassRepository storageClassRepository;
    private final StorageOverrideRepository storageOverrideRepository;

    public StorageClassesService(
            StorageClassRepository storageClassRepository,
            StorageOverrideRepository storageOverrideRepository) {
        this.storageClassRepository = storageClassRepository;
        this.storageOverrideRepository = storageOverrideRepository;
    }

    public StorageClassList listStorageClasses(Integer page, Integer perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by("name"));
        Page<StorageClassEntity> entityPage = storageClassRepository.findAll(pageable);
        StorageClassList list = new StorageClassList();
        list.setTotal((int) entityPage.getTotalElements());
        list.setPage(page);
        list.setPerPage(perPage);
        list.setItems(entityPage.getContent().stream().map(this::toApi).collect(Collectors.toList()));
        return list;
    }

    @Transactional
    public StorageClass createStorageClass(StorageClassCreate body) {
        if (storageClassRepository.existsById(body.getName())) {
            throw new IllegalArgumentException("Storage class already exists: " + body.getName());
        }
        StorageClassEntity entity = new StorageClassEntity();
        entity.setName(body.getName());
        applyCreateOrReplace(entity, body.getDescription(), body.getCapabilities(),
                body.getConstraints(), body.getMetadata());
        return toApi(storageClassRepository.save(entity));
    }

    public StorageClass getStorageClass(String name) {
        return storageClassRepository.findByName(name)
                .map(this::toApi)
                .orElseThrow(() -> new EntityNotFoundException("Storage class not found: " + name));
    }

    @Transactional
    public StorageClass replaceStorageClass(String name, StorageClassUpdate update) {
        return applyUpdate(name, update);
    }

    @Transactional
    public StorageClass updateStorageClass(String name, StorageClassUpdate update) {
        return applyUpdate(name, update);
    }

    @Transactional
    public void deleteStorageClass(String name) {
        if (!storageClassRepository.existsById(name)) {
            throw new EntityNotFoundException("Storage class not found: " + name);
        }
        storageClassRepository.deleteById(name);
    }

    public StorageClassOverrides getStorageClassOverrides(String name) {
        if (!storageClassRepository.existsById(name)) {
            throw new EntityNotFoundException("Storage class not found: " + name);
        }
        StorageClassOverrides response = new StorageClassOverrides();
        response.setStorageClassName(name);
        List<StorageOverrideEntity> rows = storageOverrideRepository.findByStorageClassName(name);
        response.setOverrides(rows.stream().map(this::overrideToApi).collect(Collectors.toList()));
        return response;
    }

    @Transactional
    public StorageClassOverrides replaceStorageClassOverrides(String name, StorageClassOverrides body) {
        if (!storageClassRepository.existsById(name)) {
            throw new EntityNotFoundException("Storage class not found: " + name);
        }
        if (!name.equals(body.getStorageClassName())) {
            throw new IllegalArgumentException(
                    "storageClassName in body must match path: " + body.getStorageClassName());
        }
        for (StorageClassOverride o : body.getOverrides()) {
            if (o.getDatacenterId() != null) {
                throw new IllegalArgumentException(
                        "datacenterId is not persisted for storage overrides in this release; omit it.");
            }
        }
        List<String> providerTypes = body.getOverrides().stream()
                .map(o -> o.getProviderType().getValue().toLowerCase())
                .toList();
        long distinct = providerTypes.stream().distinct().count();
        if (distinct != providerTypes.size()) {
            throw new IllegalArgumentException("Duplicate providerType entries are not allowed for one storage class.");
        }

        storageOverrideRepository.deleteByStorageClassName(name);

        for (StorageClassOverride o : body.getOverrides()) {
            StorageOverrideEntity entity = new StorageOverrideEntity();
            entity.setStorageClassName(name);
            entity.setProviderType(o.getProviderType().getValue().toLowerCase());
            entity.setProviderStorageNames(new ArrayList<>(o.getProviderStorageNames()));
            entity.setPriority(o.getPriority() != null ? o.getPriority() : 100);
            storageOverrideRepository.save(entity);
        }

        return getStorageClassOverrides(name);
    }

    private StorageClass applyUpdate(String name, StorageClassUpdate update) {
        StorageClassEntity entity = storageClassRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException("Storage class not found: " + name));
        if (update.getDescription() != null) {
            entity.setDescription(update.getDescription());
        }
        if (update.getCapabilities() != null) {
            entity.setCapabilities(capabilitiesToMap(update.getCapabilities()));
            entity.setTier(tierFromCapabilities(entity.getCapabilities()));
        }
        if (update.getConstraints() != null) {
            entity.setConstraints(constraintsToMap(update.getConstraints()));
        }
        if (update.getMetadata() != null) {
            Map<String, Object> features = entity.getFeatures() != null
                    ? new HashMap<>(entity.getFeatures()) : new HashMap<>();
            if (update.getMetadata().isEmpty()) {
                features.remove(FEATURES_METADATA_KEY);
            } else {
                features.put(FEATURES_METADATA_KEY, new LinkedHashMap<>(update.getMetadata()));
            }
            entity.setFeatures(features);
        }
        return toApi(storageClassRepository.save(entity));
    }

    private void applyCreateOrReplace(
            StorageClassEntity entity,
            String description,
            StorageCapabilities capabilities,
            StorageConstraints constraints,
            Map<String, String> metadata) {
        entity.setDescription(description);
        entity.setType("block");
        Map<String, Object> capMap = capabilitiesToMap(capabilities);
        entity.setCapabilities(capMap);
        entity.setTier(tierFromCapabilities(capMap));
        entity.setConstraints(constraintsToMap(
                constraints != null ? constraints : new StorageConstraints()));
        Map<String, Object> features = new HashMap<>();
        if (metadata != null && !metadata.isEmpty()) {
            features.put(FEATURES_METADATA_KEY, new LinkedHashMap<>(metadata));
        }
        entity.setFeatures(features);
    }

    private StorageClassOverride overrideToApi(StorageOverrideEntity e) {
        StorageClassOverride o = new StorageClassOverride();
        o.setProviderType(ProviderType.fromValue(e.getProviderType().toUpperCase()));
        o.setProviderStorageNames(new ArrayList<>(e.getProviderStorageNames()));
        o.setPriority(e.getPriority());
        o.setDatacenterId(null);
        return o;
    }

    private StorageClass toApi(StorageClassEntity entity) {
        StorageClass sc = new StorageClass();
        sc.setId(null);
        sc.setName(entity.getName());
        sc.setDescription(entity.getDescription());
        sc.setCapabilities(mapToCapabilities(entity.getCapabilities()));
        sc.setConstraints(mapToConstraints(entity.getConstraints()));
        sc.setMetadata(extractMetadata(entity));
        if (entity.getCreatedAt() != null) {
            sc.setCreatedAt(OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));
        }
        if (entity.getUpdatedAt() != null) {
            sc.setUpdatedAt(OffsetDateTime.ofInstant(entity.getUpdatedAt(), ZoneOffset.UTC));
        }
        return sc;
    }

    private Map<String, String> extractMetadata(StorageClassEntity entity) {
        if (entity.getFeatures() == null) {
            return new HashMap<>();
        }
        Object raw = entity.getFeatures().get(FEATURES_METADATA_KEY);
        if (raw instanceof Map<?, ?> map) {
            Map<String, String> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> en : map.entrySet()) {
                if (en.getKey() != null && en.getValue() != null) {
                    out.put(String.valueOf(en.getKey()), String.valueOf(en.getValue()));
                }
            }
            return out;
        }
        return new HashMap<>();
    }

    private static Map<String, Object> capabilitiesToMap(StorageCapabilities c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c.getPerformance() != null) {
            m.put("performance", c.getPerformance().getValue());
        }
        if (c.getMedia() != null) {
            m.put("media", c.getMedia().getValue());
        }
        if (c.getShared() != null) {
            m.put("shared", c.getShared());
        }
        if (c.getRedundancy() != null) {
            m.put("redundancy", c.getRedundancy().getValue());
        }
        return m;
    }

    private static StorageCapabilities mapToCapabilities(Map<String, Object> raw) {
        StorageCapabilities c = new StorageCapabilities();
        if (raw == null) {
            return c;
        }
        Optional.ofNullable(raw.get("performance")).map(String::valueOf).ifPresent(v -> {
            try {
                c.setPerformance(StorageCapabilities.PerformanceEnum.fromValue(v));
            } catch (IllegalArgumentException ignored) {
                // leave unset if legacy or unknown value
            }
        });
        Optional.ofNullable(raw.get("media")).map(String::valueOf).ifPresent(v -> {
            try {
                c.setMedia(StorageCapabilities.MediaEnum.fromValue(v));
            } catch (IllegalArgumentException ignored) {
            }
        });
        Object sharedRaw = raw.get("shared");
        if (sharedRaw instanceof Boolean b) {
            c.setShared(b);
        } else if (sharedRaw instanceof Number n) {
            c.setShared(n.intValue() != 0);
        }
        Optional.ofNullable(raw.get("redundancy")).map(String::valueOf).ifPresent(v -> {
            try {
                c.setRedundancy(StorageCapabilities.RedundancyEnum.fromValue(v));
            } catch (IllegalArgumentException ignored) {
            }
        });
        return c;
    }

    private static Map<String, Object> constraintsToMap(StorageConstraints c) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (c.getMinIops() != null) {
            m.put("min_iops", c.getMinIops());
        }
        if (c.getMaxLatencyMs() != null) {
            m.put("max_latency_ms", c.getMaxLatencyMs());
        }
        return m;
    }

    private static StorageConstraints mapToConstraints(Map<String, Object> raw) {
        StorageConstraints c = new StorageConstraints();
        if (raw == null) {
            return c;
        }
        if (raw.get("min_iops") instanceof Number n) {
            c.setMinIops(n.intValue());
        }
        if (raw.get("max_latency_ms") instanceof Number n) {
            c.setMaxLatencyMs(n.intValue());
        }
        return c;
    }

    private static String tierFromCapabilities(Map<String, Object> cap) {
        if (cap == null) {
            return "balanced";
        }
        String perf = cap.get("performance") != null ? String.valueOf(cap.get("performance")) : "";
        return switch (perf) {
            case "high" -> "performance";
            case "low" -> "capacity";
            default -> "balanced";
        };
    }

}
