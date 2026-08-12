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
package com.yorel.muxon.worker.plugin;

import com.yorel.muxon.api.enums.ResourceTypeStatus;
import com.yorel.muxon.db.model.ResourceTypeDefinitionEntity;
import com.yorel.muxon.db.repository.PluginRepository;
import com.yorel.muxon.db.repository.ResourceTypeDefinitionRepository;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * In-memory cache of plugin-registered resource types. Loaded at startup and refreshed on plugin
 * activation/deactivation events.
 *
 * <p>The {@code vm} kind is reserved; it is never stored in this registry and resolveKind("vm")
 * always returns empty.
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
    resourceTypeDefinitionRepository
        .findAll()
        .forEach(
            rtd -> {
              if (rtd.getStatus() == ResourceTypeStatus.ACTIVE) {
                cache.put(rtd.getKind(), toRouteTarget(rtd));
              }
            });
    log.info("ResourceTypeRegistry loaded {} active kinds", cache.size());
  }

  /**
   * Resolves a resource kind to its owning plugin's route target. Returns empty for the reserved
   * 'vm' kind or any unregistered kind.
   */
  public Optional<PluginRouteTarget> resolveKind(String kind) {
    if (RESERVED_KINDS.contains(kind)) {
      return Optional.empty();
    }
    return Optional.ofNullable(cache.get(kind));
  }

  @EventListener
  public void onPluginLifecycleEvent(com.yorel.muxon.db.plugin.PluginLifecycleEvent event) {
    reload();
  }

  private PluginRouteTarget toRouteTarget(ResourceTypeDefinitionEntity rtd) {
    return new PluginRouteTarget(
        rtd.getPlugin().getId(),
        rtd.getPlugin().getName(),
        rtd.getPlugin().getGrpcAddress(),
        rtd.getKind(),
        rtd.getStatus());
  }
}
