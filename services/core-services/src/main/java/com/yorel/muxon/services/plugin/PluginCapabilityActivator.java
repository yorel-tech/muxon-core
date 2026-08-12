/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.services.plugin;

import com.yorel.muxon.api.enums.CatalogContributionStatus;
import com.yorel.muxon.api.enums.PluginCapabilityType;
import com.yorel.muxon.api.enums.ResourceTypeStatus;
import com.yorel.muxon.db.model.PluginCapabilityEntity;
import com.yorel.muxon.db.model.PluginCatalogContributionEntity;
import com.yorel.muxon.db.model.PluginEntity;
import com.yorel.muxon.db.model.PluginUiModuleEntity;
import com.yorel.muxon.db.model.ResourceTypeDefinitionEntity;
import com.yorel.muxon.db.repository.PluginCapabilityRepository;
import com.yorel.muxon.db.repository.PluginCatalogContributionRepository;
import com.yorel.muxon.db.repository.PluginUiModuleRepository;
import com.yorel.muxon.db.repository.ResourceTypeDefinitionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Performs the per-capability activation steps after a plugin passes its initial health check.
 * Called by PluginService.activatePlugin as part of the activation flow.
 */
@Service
public class PluginCapabilityActivator {

  private static final Logger log = LoggerFactory.getLogger(PluginCapabilityActivator.class);

  private static final Set<String> RESERVED_KINDS = Set.of("vm");

  @Value("${muxon.plugins.catalog.approval-required:false}")
  private boolean catalogApprovalRequired;

  @Autowired private PluginCapabilityRepository capabilityRepository;
  @Autowired private ResourceTypeDefinitionRepository resourceTypeDefinitionRepository;
  @Autowired private PluginUiModuleRepository uiModuleRepository;
  @Autowired private PluginCatalogContributionRepository catalogContributionRepository;
  @Autowired private PluginGrpcChannelFactory channelFactory;

  /**
   * Calls DescribeCapabilities on the plugin, validates the response against manifest declarations,
   * then registers resource types, RBAC permissions, UI modules, and catalog contributions. Throws
   * if any mismatch is found — the entire activation is aborted.
   */
  @Transactional
  public void activateAll(PluginEntity plugin) {
    List<PluginCapabilityEntity> capabilities = capabilityRepository.findByPluginId(plugin.getId());

    Set<String> implementedServices = channelFactory.describeCapabilities(plugin);

    for (PluginCapabilityEntity cap : capabilities) {
      String expectedService = toServiceName(cap.getCapabilityType());
      if (!implementedServices.contains(expectedService)) {
        throw new PluginRegistrationException(
            "Plugin "
                + plugin.getName()
                + " declared capability "
                + cap.getCapabilityType()
                + " but DescribeCapabilities did not list "
                + expectedService);
      }

      switch (cap.getCapabilityType()) {
        case RESOURCE_PROVIDER -> activateResourceProvider(plugin, cap);
        case UI_EXTENSION -> activateUiExtension(plugin, cap);
        case SERVICE, RUNTIME -> activateCatalogContributions(plugin, cap);
        default ->
            throw new PluginRegistrationException(
                "Unsupported capability: " + cap.getCapabilityType());
      }
    }

    log.info("All capabilities activated for plugin {}:{}", plugin.getName(), plugin.getVersion());
  }

  @SuppressWarnings("unchecked")
  private void activateResourceProvider(PluginEntity plugin, PluginCapabilityEntity cap) {
    Map<String, Object> config = cap.getConfig();
    if (config == null) return;

    Object kindsObj = config.get("kinds");
    if (!(kindsObj instanceof List<?> kinds)) return;

    for (Object kindObj : kinds) {
      String kind = kindObj.toString();

      if (RESERVED_KINDS.contains(kind)) {
        throw new PluginRegistrationException(
            "Kind '" + kind + "' is reserved and cannot be claimed by a plugin");
      }

      Optional<ResourceTypeDefinitionEntity> existing =
          resourceTypeDefinitionRepository.findByKind(kind);
      if (existing.isPresent()
          && existing.get().getStatus() == ResourceTypeStatus.ACTIVE
          && !existing.get().getPlugin().getId().equals(plugin.getId())) {
        throw new PluginRegistrationException(
            "Kind '"
                + kind
                + "' is already owned by an active plugin: "
                + existing.get().getPlugin().getName());
      }

      ResourceTypeDefinitionEntity rtd =
          existing
              .filter(e -> e.getPlugin().getId().equals(plugin.getId()))
              .orElseGet(ResourceTypeDefinitionEntity::new);

      rtd.setPlugin(plugin);
      rtd.setKind(kind);
      rtd.setPluralKind(kind + "s");
      rtd.setSchema(extractSchema(config, kind));
      rtd.setSupportedOperations(extractOperations(config));
      rtd.setStatus(ResourceTypeStatus.ACTIVE);
      resourceTypeDefinitionRepository.save(rtd);

      log.info(
          "Registered resource kind '{}' for plugin {}:{}",
          kind,
          plugin.getName(),
          plugin.getVersion());
    }
  }

  @SuppressWarnings("unchecked")
  private void activateUiExtension(PluginEntity plugin, PluginCapabilityEntity cap) {
    Map<String, Object> config = cap.getConfig();
    if (config == null) return;

    Object modulesObj = config.get("modules");
    if (!(modulesObj instanceof List<?> modules)) return;

    for (Object modObj : modules) {
      Map<String, Object> mod = (Map<String, Object>) modObj;
      String moduleId = (String) mod.get("id");

      PluginUiModuleEntity entity =
          uiModuleRepository.findByPluginId(plugin.getId()).stream()
              .filter(m -> moduleId.equals(m.getModuleId()))
              .findFirst()
              .orElseGet(PluginUiModuleEntity::new);

      entity.setPlugin(plugin);
      entity.setModuleId(moduleId);
      entity.setDisplayName((String) mod.getOrDefault("displayName", moduleId));
      entity.setModuleUrl((String) mod.get("moduleUrl"));

      Object permsObj = mod.get("requiredPermissions");
      entity.setRequiredPermissions(
          permsObj instanceof List<?> perms
              ? perms.stream().map(Object::toString).toList()
              : List.of());

      Object kindsObj = mod.get("targetKinds");
      entity.setTargetKinds(
          kindsObj instanceof List<?> kinds
              ? kinds.stream().map(Object::toString).toList()
              : List.of());

      uiModuleRepository.save(entity);
      log.info(
          "Registered UI module '{}' for plugin {}:{}",
          moduleId,
          plugin.getName(),
          plugin.getVersion());
    }
  }

  @SuppressWarnings("unchecked")
  private void activateCatalogContributions(PluginEntity plugin, PluginCapabilityEntity cap) {
    Map<String, Object> config = cap.getConfig();
    if (config == null) return;

    Object catalogObj = config.get("catalog");
    if (!(catalogObj instanceof List<?> catalogItems)) return;

    CatalogContributionStatus initialStatus =
        catalogApprovalRequired
            ? CatalogContributionStatus.PENDING_APPROVAL
            : CatalogContributionStatus.ACTIVE;

    for (Object itemObj : catalogItems) {
      Map<String, Object> item = (Map<String, Object>) itemObj;
      String name = (String) item.get("name");

      PluginCatalogContributionEntity entity =
          catalogContributionRepository.findByPluginId(plugin.getId()).stream()
              .filter(c -> name.equals(c.getName()))
              .findFirst()
              .orElseGet(PluginCatalogContributionEntity::new);

      entity.setPlugin(plugin);
      entity.setName(name);
      entity.setDisplayName((String) item.getOrDefault("displayName", name));
      entity.setDescription((String) item.get("description"));
      entity.setCatalogItemType(parseCatalogItemType((String) item.get("type")));
      entity.setTargetCapability((String) item.get("targetCapability"));
      entity.setConfigSchema(
          item.containsKey("configSchema")
              ? (Map<String, Object>) item.get("configSchema")
              : Map.of());
      entity.setStatus(initialStatus);
      catalogContributionRepository.save(entity);
      log.info(
          "Registered catalog contribution '{}' for plugin {}:{}",
          name,
          plugin.getName(),
          plugin.getVersion());
    }
  }

  private String toServiceName(PluginCapabilityType type) {
    return switch (type) {
      case RUNTIME -> "RuntimeCapabilityService";
      case SERVICE -> "ServiceCapabilityService";
      case RESOURCE_PROVIDER -> "ResourceReconcileService";
      case UI_EXTENSION -> "UiExtensionService";
    };
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractSchema(Map<String, Object> config, String kind) {
    Object schemas = config.get("schemas");
    if (schemas instanceof Map<?, ?> m && m.containsKey(kind)) {
      return (Map<String, Object>) m.get(kind);
    }
    return Map.of();
  }

  @SuppressWarnings("unchecked")
  private List<String> extractOperations(Map<String, Object> config) {
    Object ops = config.get("supportedOperations");
    if (ops instanceof List<?> list) {
      return list.stream().map(Object::toString).toList();
    }
    return List.of("CREATE", "READ", "DELETE", "LIST");
  }

  private com.yorel.muxon.api.enums.CatalogItemType parseCatalogItemType(String type) {
    return switch (type == null ? "" : type.toLowerCase()) {
      case "runtime-offering" -> com.yorel.muxon.api.enums.CatalogItemType.RUNTIME_OFFERING;
      case "stack-blueprint" -> com.yorel.muxon.api.enums.CatalogItemType.STACK_BLUEPRINT;
      default -> com.yorel.muxon.api.enums.CatalogItemType.SERVICE_OFFERING;
    };
  }
}
