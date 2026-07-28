package com.scal.muxon.worker.plugin;

import com.scal.muxon.api.enums.ResourceTypeStatus;
import com.scal.muxon.db.model.ResourceTypeDefinitionEntity;
import com.scal.muxon.db.repository.PluginRepository;
import com.scal.muxon.db.repository.ResourceTypeDefinitionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache of plugin-registered resource types.
 * Loaded at startup and refreshed on plugin activation/deactivation events.
 *
 * <p>The {@code vm} kind is reserved; it is never stored in this registry
 * and resolveKind("vm") always returns empty.
 */
@Component
public class ResourceTypeRegistry {

    private static final Logger log = LoggerFactory.getLogger(ResourceTypeRegistry.class);

    private static final Set<String> RESERVED_KINDS = Set.of("vm");

    private final Map<String, PluginRouteTarget> cache = new ConcurrentHashMap<>();

    @Autowired private ResourceTypeDefinitionRepository resourceTypeDefinitionRepository;
    @Autowired private PluginRepository pluginRepository;

    @PostConstruct
    public void load() {
        reload();
    }

    public void reload() {
        cache.clear();
        resourceTypeDefinitionRepository.findAll().forEach(rtd -> {
            if (rtd.getStatus() == ResourceTypeStatus.ACTIVE) {
                cache.put(rtd.getKind(), toRouteTarget(rtd));
            }
        });
        log.info("ResourceTypeRegistry loaded {} active kinds", cache.size());
    }

    /**
     * Resolves a resource kind to its owning plugin's route target.
     * Returns empty for the reserved 'vm' kind or any unregistered kind.
     */
    public Optional<PluginRouteTarget> resolveKind(String kind) {
        if (RESERVED_KINDS.contains(kind)) {
            return Optional.empty();
        }
        return Optional.ofNullable(cache.get(kind));
    }

    @EventListener
    public void onPluginLifecycleEvent(com.scal.muxon.db.plugin.PluginLifecycleEvent event) {
        reload();
    }

    private PluginRouteTarget toRouteTarget(ResourceTypeDefinitionEntity rtd) {
        return new PluginRouteTarget(
                rtd.getPlugin().getId(),
                rtd.getPlugin().getName(),
                rtd.getPlugin().getGrpcAddress(),
                rtd.getKind(),
                rtd.getStatus()
        );
    }
}
