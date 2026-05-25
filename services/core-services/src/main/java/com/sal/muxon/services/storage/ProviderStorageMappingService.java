package com.sal.muxon.services.storage;

import com.sal.muxon.api.model.MappedStorageClassInfo;
import com.sal.muxon.db.model.ProviderStorageEntity;
import com.sal.muxon.db.model.StorageClassEntity;
import com.sal.muxon.db.model.StorageOverrideEntity;
import com.sal.muxon.common.EntityNotFoundException;
import com.sal.muxon.db.repository.StorageClassRepository;
import com.sal.muxon.db.repository.StorageOverrideRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Computes which storage classes apply to a provider storage row (scheduler-style capability match
 * plus manual overrides) and supports filtering inventory by storage class name.
 */
@Service
public class ProviderStorageMappingService {

    private final StorageClassRepository storageClassRepository;
    private final StorageOverrideRepository storageOverrideRepository;
    private final ProviderStorageClassMappingEvaluator capabilityEvaluator;

    public ProviderStorageMappingService(
            StorageClassRepository storageClassRepository,
            StorageOverrideRepository storageOverrideRepository,
            ProviderStorageClassMappingEvaluator capabilityEvaluator) {
        this.storageClassRepository = storageClassRepository;
        this.storageOverrideRepository = storageOverrideRepository;
        this.capabilityEvaluator = capabilityEvaluator;
    }

    /**
     * Snapshot of classes and overrides for batch mapping (single DB round-trip each).
     */
    public MappingBatchContext loadBatchContext() {
        return new MappingBatchContext(
                storageClassRepository.findAll(),
                storageOverrideRepository.findAll());
    }

    /** @throws EntityNotFoundException if the name is not a defined storage class */
    public void requireStorageClassExists(String storageClassName) {
        if (storageClassName == null || storageClassName.isBlank()) {
            return;
        }
        String n = storageClassName.trim();
        if (!storageClassRepository.existsById(n)) {
            throw new EntityNotFoundException("Storage class not found: " + n);
        }
    }

    public List<MappedStorageClassInfo> computeMappedClasses(
            ProviderStorageEntity storage, MappingBatchContext ctx) {
        LinkedHashMap<String, MappedStorageClassInfo.MappingSourceEnum> byName = new LinkedHashMap<>();
        for (StorageClassEntity cls : ctx.storageClasses()) {
            if (capabilityEvaluator.matchesByCapabilities(cls, storage)) {
                byName.put(cls.getName(), MappedStorageClassInfo.MappingSourceEnum.SCHEDULER);
            }
        }
        String pt = storage.getProviderType();
        if (pt != null) {
            String ptNorm = pt.toLowerCase(Locale.ROOT);
            for (StorageOverrideEntity o : ctx.overridesForProviderType(ptNorm)) {
                if (overrideMatchesStorage(o, storage)) {
                    // Manual override wins over capability-only mapping for the same class name.
                    byName.put(o.getStorageClassName(), MappedStorageClassInfo.MappingSourceEnum.OVERRIDE);
                }
            }
        }
        return byName.entrySet().stream()
                .map(e -> {
                    MappedStorageClassInfo m = new MappedStorageClassInfo();
                    m.setStorageClassName(e.getKey());
                    m.setMappingSource(e.getValue());
                    return m;
                })
                .sorted(Comparator.comparing(MappedStorageClassInfo::getStorageClassName))
                .collect(Collectors.toList());
    }

    public boolean isMappedToStorageClass(
            ProviderStorageEntity storage, String storageClassName, MappingBatchContext ctx) {
        StorageClassEntity cls = ctx.classByName(storageClassName);
        if (cls != null && capabilityEvaluator.matchesByCapabilities(cls, storage)) {
            return true;
        }
        for (StorageOverrideEntity o : ctx.overridesForClass(storageClassName)) {
            if (overrideMatchesStorage(o, storage)) {
                return true;
            }
        }
        return false;
    }

    private static boolean overrideMatchesStorage(StorageOverrideEntity o, ProviderStorageEntity storage) {
        if (o.getProviderType() == null
                || storage.getProviderType() == null
                || !o.getProviderType().equalsIgnoreCase(storage.getProviderType())) {
            return false;
        }
        List<String> names = o.getProviderStorageNames();
        if (names == null || names.isEmpty()) {
            return false;
        }
        String poolName = storage.getName();
        String ext = storage.getExternalId();
        for (String n : names) {
            if (n == null) {
                continue;
            }
            if (Objects.equals(n, poolName) || Objects.equals(n, ext)) {
                return true;
            }
        }
        return false;
    }

    public record MappingBatchContext(
            List<StorageClassEntity> storageClasses,
            List<StorageOverrideEntity> allOverrides) {

        Map<String, StorageClassEntity> classByName() {
            return storageClasses.stream().collect(Collectors.toMap(StorageClassEntity::getName, c -> c));
        }

        StorageClassEntity classByName(String name) {
            return classByName().get(name);
        }

        List<StorageOverrideEntity> overridesForProviderType(String providerTypeLower) {
            List<StorageOverrideEntity> out = new ArrayList<>();
            for (StorageOverrideEntity o : allOverrides) {
                if (o.getProviderType() != null
                        && o.getProviderType().toLowerCase(Locale.ROOT).equals(providerTypeLower)) {
                    out.add(o);
                }
            }
            return out;
        }

        List<StorageOverrideEntity> overridesForClass(String storageClassName) {
            List<StorageOverrideEntity> out = new ArrayList<>();
            for (StorageOverrideEntity o : allOverrides) {
                if (storageClassName.equals(o.getStorageClassName())) {
                    out.add(o);
                }
            }
            return out;
        }
    }
}
