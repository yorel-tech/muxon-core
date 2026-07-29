package com.yorel.muxon.info;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides all capabilities and modules for the core product.
 * Used only in muxon-core; basic VM operations are implicit in compute modules.
 */
@Component
public class CoreCapabilityProvider implements CapabilityProvider, ModuleProvider, InitializingBean {

    private final String modulesConfig;
    private final List<ModuleDescriptor> modules = new ArrayList<>();

    public CoreCapabilityProvider(@Value("${muxon.modules:}") String modulesConfig) {
        this.modulesConfig = modulesConfig != null ? modulesConfig : "";
    }

    @Override
    public void afterPropertiesSet() {
        if (modulesConfig.isBlank()) {
            return;
        }
        for (String rawId : modulesConfig.split(",")) {
            String id = rawId.trim();
            if (id.isEmpty()) continue;
            String category = id.contains(".") ? id.substring(0, id.indexOf('.')) : "other";
            Map<String, String> metadata = new HashMap<>();
            modules.add(ModuleDescriptor.builder()
                    .id(id)
                    .displayName(id)
                    .category(category)
                    .builtin(true)
                    .metadata(metadata)
                    .build());
        }
    }

    @Override
    public Set<String> getCapabilities() {
        Set<String> capabilities = new HashSet<>();
        capabilities.add("monitoring.metrics");
        return capabilities;
    }

    @Override
    public List<ModuleDescriptor> getModules() {
        return List.copyOf(modules);
    }
}
