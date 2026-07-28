package com.scal.muxon.info;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Aggregates modules from all {@link ModuleProvider}s and exposes
 * a simple registry API for the rest of the application.
 */
@Component
public class ModuleRegistry {

    private final List<ModuleProvider> providers;

    private volatile List<ModuleDescriptor> cachedModules = Collections.emptyList();
    private volatile Map<String, ModuleDescriptor> cachedById = Collections.emptyMap();

    public ModuleRegistry(List<ModuleProvider> providers) {
        this.providers = providers;
        refreshCache();
    }

    /**
     * Return all known modules.
     */
    public List<ModuleDescriptor> getAllModules() {
        return cachedModules;
    }

    /**
     * Find a module by id, if present.
     */
    public Optional<ModuleDescriptor> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cachedById.get(id));
    }

    /**
     * Refresh the internal cache from all providers.
     * <p>
     * This can be made public or wired to configuration reload hooks in future.
     */
    void refreshCache() {
        Map<String, ModuleDescriptor> byId = new HashMap<>();
        for (ModuleProvider provider : providers) {
            for (ModuleDescriptor descriptor : provider.getModules()) {
                if (descriptor.getId() != null) {
                    byId.put(descriptor.getId(), descriptor);
                }
            }
        }
        this.cachedById = Collections.unmodifiableMap(byId);
        this.cachedModules = List.copyOf(byId.values());
    }
}

