package com.scal.muxon.worker.plugin;

import com.scal.muxon.api.enums.PluginSource;
import com.scal.muxon.api.enums.PluginStatus;
import com.scal.muxon.db.model.PluginCapabilityEntity;
import com.scal.muxon.db.model.PluginEntity;
import com.scal.muxon.db.repository.PluginCapabilityRepository;
import com.scal.muxon.db.repository.PluginRepository;
import com.scal.muxon.providers.VmProvider;
import com.scal.muxon.worker.wiring.TenantAwareVmProviderRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Bootstraps built-in hypervisor providers (libvirt, proxmox) as BUILTIN-source
 * plugin registry entries at startup. Runs after Flyway migrations and after
 * TenantAwareVmProviderRegistry is initialised.
 *
 * <p>BUILTIN plugins are always ACTIVE; they are not managed via the plugin lifecycle
 * API and cannot be deleted. Health polling is skipped for them (see PluginHealthMonitorService).
 */
@Component
@DependsOn("flywayInitializer")
public class BuiltinPluginBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(BuiltinPluginBootstrapper.class);

    @Autowired private PluginRepository pluginRepository;
    @Autowired private PluginCapabilityRepository capabilityRepository;
    @Autowired private TenantAwareVmProviderRegistry vmProviderRegistry;

    private static final List<String> BUILTIN_PROVIDER_IDS = List.of("libvirt", "proxmox");

    @PostConstruct
    @Transactional
    public void bootstrap() {
        for (String providerId : BUILTIN_PROVIDER_IDS) {
            ensureBuiltinPlugin(providerId);
        }
    }

    private void ensureBuiltinPlugin(String providerId) {
        boolean exists = pluginRepository.existsByNameAndVersion(providerId, "builtin");
        if (exists) {
            return;
        }

        PluginEntity plugin = new PluginEntity();
        plugin.setName(providerId);
        plugin.setVersion("builtin");
        plugin.setSource(PluginSource.BUILTIN);
        plugin.setStatus(PluginStatus.ACTIVE);
        plugin.setManifest(Map.of(
                "apiVersion", "platform.io/v1",
                "kind", "Plugin",
                "metadata", Map.of("name", providerId, "version", "builtin"),
                "spec", Map.of("capabilities", List.of(Map.of("type", "runtime", "name", providerId)))
        ));
        plugin = pluginRepository.save(plugin);

        PluginCapabilityEntity cap = new PluginCapabilityEntity();
        cap.setPlugin(plugin);
        cap.setCapabilityType(com.scal.muxon.api.enums.PluginCapabilityType.RUNTIME);
        cap.setCapabilityName(providerId);
        capabilityRepository.save(cap);

        log.info("Bootstrapped BUILTIN plugin: {}", providerId);
    }
}
