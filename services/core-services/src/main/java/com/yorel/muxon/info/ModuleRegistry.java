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
package com.yorel.muxon.info;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Aggregates modules from all {@link ModuleProvider}s and exposes a simple registry API for the
 * rest of the application.
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

  /** Return all known modules. */
  public List<ModuleDescriptor> getAllModules() {
    return cachedModules;
  }

  /** Find a module by id, if present. */
  public Optional<ModuleDescriptor> findById(String id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(cachedById.get(id));
  }

  /**
   * Refresh the internal cache from all providers.
   *
   * <p>This can be made public or wired to configuration reload hooks in future.
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
