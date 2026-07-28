package com.yorel.muxon.services.storage;

import com.yorel.muxon.api.model.ProviderStorage;
import com.yorel.muxon.api.model.ProviderStorageCapabilities;
import com.yorel.muxon.api.model.ProviderStorageList;
import com.yorel.muxon.api.model.ProviderStorageMetrics;
import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.db.model.ProviderStorageEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProviderStorageApiConverter {

    private final ProviderStorageMappingService mappingService;

    public ProviderStorageApiConverter(ProviderStorageMappingService mappingService) {
        this.mappingService = mappingService;
    }

    public ProviderStorage toApi(ProviderStorageEntity entity, ProviderStorageMappingService.MappingBatchContext ctx) {
        ProviderType providerType = ProviderType.valueOf(entity.getProviderType().toUpperCase(Locale.ROOT));
        ProviderStorage api = new ProviderStorage(
                entity.getId(),
                entity.getProviderId(),
                providerType,
                entity.getExternalId(),
                entity.getStorageType(),
                mapStorageCapabilities(entity.getCapabilities()),
                mapStorageMetrics(entity.getMetrics()));
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
        api.setMappedStorageClasses(mappingService.computeMappedClasses(entity, ctx));
        return api;
    }

    public ProviderStorageList toPagedList(
            List<ProviderStorageEntity> filtered,
            Integer page,
            Integer perPage,
            ProviderStorageMappingService.MappingBatchContext ctx) {
        int total = filtered.size();
        int p = page != null ? page : 1;
        int pp = perPage != null ? perPage : 20;
        int from = Math.max(0, (p - 1) * pp);
        int to = Math.min(from + pp, total);
        List<ProviderStorage> items = from < total
                ? filtered.subList(from, to).stream().map(e -> toApi(e, ctx)).toList()
                : List.of();
        ProviderStorageList list = new ProviderStorageList();
        list.setTotal(total);
        list.setPage(p);
        list.setPerPage(pp);
        list.setItems(items);
        return list;
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
