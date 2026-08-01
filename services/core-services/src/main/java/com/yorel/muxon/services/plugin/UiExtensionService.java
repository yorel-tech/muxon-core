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

import com.yorel.muxon.api.enums.PluginStatus;
import com.yorel.muxon.db.model.PluginEntity;
import com.yorel.muxon.db.repository.PluginRepository;
import com.yorel.muxon.db.repository.PluginUiModuleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Returns the UI extension manifest for a given user/tenant context.
 *
 * <p>Filters modules where:
 *
 * <ol>
 *   <li>The owning plugin is {@code ACTIVE}.
 *   <li>The plugin is published to the requesting tenant (publication scope check).
 *   <li>All {@code requiredPermissions} are satisfied by the user's effective permission set.
 * </ol>
 *
 * <p>Responses are cached per (tenantId + permissions fingerprint) with a short TTL to avoid
 * per-request DB queries (cache configured in application.yaml via Caffeine).
 */
@Service
public class UiExtensionService {

  @Autowired private PluginRepository pluginRepository;
  @Autowired private PluginUiModuleRepository uiModuleRepository;

  public record UiModuleEntry(
      String id, String displayName, String moduleUrl, List<String> targetKinds) {}

  /**
   * Lists extension modules visible to {@code userPermissions} for the given tenant.
   *
   * @param tenantId the requesting tenant's ID
   * @param userPermissions the set of permission strings the user holds for this tenant
   * @return list of module entries containing only safe-to-expose metadata (no plugin internals)
   */
  @Cacheable(
      value = "uiExtensionManifest",
      key = "#tenantId + ':' + T(java.util.Objects).hash(#userPermissions)")
  public List<UiModuleEntry> listExtensionsForUser(UUID tenantId, Set<String> userPermissions) {
    List<PluginEntity> activePlugins = pluginRepository.findByStatus(PluginStatus.ACTIVE);

    List<UUID> publishedPluginIds =
        activePlugins.stream()
            .filter(p -> isPublishedToTenant(p, tenantId))
            .map(PluginEntity::getId)
            .toList();

    if (publishedPluginIds.isEmpty()) {
      return List.of();
    }

    return uiModuleRepository.findByPluginIdIn(publishedPluginIds).stream()
        .filter(module -> userHasAllPermissions(module.getRequiredPermissions(), userPermissions))
        .map(
            module ->
                new UiModuleEntry(
                    module.getModuleId(),
                    module.getDisplayName(),
                    module.getModuleUrl(),
                    module.getTargetKinds() != null ? module.getTargetKinds() : List.of()))
        .toList();
  }

  /**
   * Publication scope check. In the base implementation all ACTIVE plugins are published to all
   * tenants (publication scope ALL by default). Enterprise overrides this to consult
   * plugin_publications.
   */
  protected boolean isPublishedToTenant(PluginEntity plugin, UUID tenantId) {
    return true;
  }

  private boolean userHasAllPermissions(List<String> required, Set<String> userPermissions) {
    if (required == null || required.isEmpty()) {
      return true;
    }
    return userPermissions.containsAll(required);
  }
}
