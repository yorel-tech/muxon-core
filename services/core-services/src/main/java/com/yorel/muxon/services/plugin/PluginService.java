package com.yorel.muxon.services.plugin;

import com.yorel.muxon.api.enums.CatalogContributionStatus;
import com.yorel.muxon.api.enums.PluginSource;
import com.yorel.muxon.api.enums.PluginStatus;
import com.yorel.muxon.api.enums.ResourceTypeStatus;
import com.yorel.muxon.db.model.PluginCapabilityEntity;
import com.yorel.muxon.db.model.PluginCatalogContributionEntity;
import com.yorel.muxon.db.model.PluginEntity;
import com.yorel.muxon.db.model.ResourceTypeDefinitionEntity;
import com.yorel.muxon.db.repository.PluginCapabilityRepository;
import com.yorel.muxon.db.repository.PluginCatalogContributionRepository;
import com.yorel.muxon.db.plugin.PluginLifecycleEvent;
import com.yorel.muxon.db.repository.PluginRepository;
import com.yorel.muxon.db.repository.ResourceTypeDefinitionRepository;
import com.yorel.muxon.services.plugin.PluginManifestValidator.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PluginService {

    private static final Logger log = LoggerFactory.getLogger(PluginService.class);

    @Autowired private PluginRepository pluginRepository;
    @Autowired private PluginCapabilityRepository capabilityRepository;
    @Autowired private ResourceTypeDefinitionRepository resourceTypeDefinitionRepository;
    @Autowired private PluginCatalogContributionRepository catalogContributionRepository;
    @Autowired private PluginManifestValidator manifestValidator;
    @Autowired private ApplicationEventPublisher eventPublisher;

    // ── Registration ────────────────────────────────────────────────────────────

    @Transactional
    public PluginEntity registerPlugin(Map<String, Object> manifestMap) {
        ValidationResult validation = manifestValidator.validate(manifestMap);
        if (!validation.valid()) {
            throw new PluginRegistrationException("Manifest validation failed: " + validation.errors());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) manifestMap.get("metadata");
        String name = (String) metadata.get("name");
        String version = (String) metadata.get("version");

        if (pluginRepository.existsByNameAndVersion(name, version)) {
            throw new PluginDuplicateException("Plugin " + name + ":" + version + " is already registered");
        }

        PluginEntity plugin = new PluginEntity();
        plugin.setName(name);
        plugin.setVersion(version);
        plugin.setSource(PluginSource.EXTERNAL);
        plugin.setStatus(PluginStatus.REGISTERED);
        plugin.setManifest(manifestMap);

        @SuppressWarnings("unchecked")
        Map<String, Object> spec = (Map<String, Object>) manifestMap.get("spec");
        extractGrpcAddress(spec, plugin);

        plugin = pluginRepository.save(plugin);

        persistCapabilities(plugin, spec);

        log.info("Plugin registered: {}:{} id={}", name, version, plugin.getId());
        return plugin;
    }

    // ── Activation ──────────────────────────────────────────────────────────────

    @Transactional
    public PluginEntity activatePlugin(UUID pluginId) {
        PluginEntity plugin = requirePlugin(pluginId);
        // Health check is performed by PluginHealthMonitor; activation delegates to it.
        // Full activation also calls PluginCapabilityActivator; both are wired separately.
        plugin.setStatus(PluginStatus.ACTIVE);
        plugin = pluginRepository.save(plugin);
        eventPublisher.publishEvent(new PluginLifecycleEvent(this, plugin, "ACTIVATED"));
        log.info("Plugin activated: {}:{} id={}", plugin.getName(), plugin.getVersion(), pluginId);
        return plugin;
    }

    // ── Disable ─────────────────────────────────────────────────────────────────

    @Transactional
    public PluginEntity disablePlugin(UUID pluginId) {
        PluginEntity plugin = requirePlugin(pluginId);
        plugin.setStatus(PluginStatus.DISABLED);
        plugin = pluginRepository.save(plugin);

        resourceTypeDefinitionRepository.findByPluginId(pluginId)
                .forEach(rtd -> {
                    rtd.setStatus(ResourceTypeStatus.UNAVAILABLE);
                    resourceTypeDefinitionRepository.save(rtd);
                });

        catalogContributionRepository.findByPluginId(pluginId)
                .forEach(contrib -> {
                    contrib.setStatus(CatalogContributionStatus.HIDDEN);
                    catalogContributionRepository.save(contrib);
                });

        eventPublisher.publishEvent(new PluginLifecycleEvent(this, plugin, "DISABLED"));
        log.info("Plugin disabled: {}:{} id={}", plugin.getName(), plugin.getVersion(), pluginId);
        return plugin;
    }

    // ── Delete ──────────────────────────────────────────────────────────────────

    @Transactional
    public void deletePlugin(UUID pluginId) {
        PluginEntity plugin = requirePlugin(pluginId);

        if (PluginSource.BUILTIN == plugin.getSource()) {
            throw new PluginRegistrationException("Built-in plugins cannot be deleted");
        }

        List<ResourceTypeDefinitionEntity> rtds = resourceTypeDefinitionRepository.findByPluginId(pluginId);
        boolean hasActiveResources = rtds.stream()
                .anyMatch(rtd -> rtd.getStatus() == ResourceTypeStatus.ACTIVE);
        if (hasActiveResources) {
            throw new PluginRegistrationException(
                    "Plugin has active resource type definitions; disable it and drain resources before deletion");
        }

        pluginRepository.delete(plugin);
        log.info("Plugin deleted: {}:{} id={}", plugin.getName(), plugin.getVersion(), pluginId);
    }

    // ── Queries ─────────────────────────────────────────────────────────────────

    public List<PluginEntity> listPlugins() {
        return pluginRepository.findAll();
    }

    public PluginEntity getPlugin(UUID pluginId) {
        return requirePlugin(pluginId);
    }

    public List<PluginCapabilityEntity> listCapabilities(UUID pluginId) {
        requirePlugin(pluginId);
        return capabilityRepository.findByPluginId(pluginId);
    }

    // ── Internal helpers ────────────────────────────────────────────────────────

    private PluginEntity requirePlugin(UUID pluginId) {
        return pluginRepository.findById(pluginId)
                .orElseThrow(() -> new PluginNotFoundException("Plugin not found: " + pluginId));
    }

    @SuppressWarnings("unchecked")
    private void extractGrpcAddress(Map<String, Object> spec, PluginEntity plugin) {
        Object grpcObj = spec.get("grpc");
        if (grpcObj instanceof Map<?, ?> grpcMap) {
            Object addr = grpcMap.get("address");
            if (addr != null) {
                plugin.setGrpcAddress(addr.toString());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void persistCapabilities(PluginEntity plugin, Map<String, Object> spec) {
        Object capsObj = spec.get("capabilities");
        if (!(capsObj instanceof List<?> caps)) return;

        for (Object capObj : caps) {
            Map<String, Object> capMap = (Map<String, Object>) capObj;
            String type = (String) capMap.get("type");
            String capName = capMap.containsKey("name") ? (String) capMap.get("name") : type;

            com.yorel.muxon.api.enums.PluginCapabilityType capType = switch (type) {
                case "runtime"           -> com.yorel.muxon.api.enums.PluginCapabilityType.RUNTIME;
                case "service"           -> com.yorel.muxon.api.enums.PluginCapabilityType.SERVICE;
                case "resource-provider" -> com.yorel.muxon.api.enums.PluginCapabilityType.RESOURCE_PROVIDER;
                case "ui-extension"      -> com.yorel.muxon.api.enums.PluginCapabilityType.UI_EXTENSION;
                default -> throw new IllegalArgumentException("Unknown capability type: " + type);
            };

            PluginCapabilityEntity cap = new PluginCapabilityEntity();
            cap.setPlugin(plugin);
            cap.setCapabilityType(capType);
            cap.setCapabilityName(capName);
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) capMap.get("config");
            cap.setConfig(config);
            capabilityRepository.save(cap);
        }
    }
}
