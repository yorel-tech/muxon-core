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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Provides all capabilities and modules for the core product. Used only in muxon-core; basic VM
 * operations are implicit in compute modules.
 */
@Component
public class CoreCapabilityProvider
    implements CapabilityProvider, ModuleProvider, InitializingBean {

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
      modules.add(
          ModuleDescriptor.builder()
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
