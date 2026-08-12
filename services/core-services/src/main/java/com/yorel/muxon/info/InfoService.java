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

import com.yorel.muxon.api.dto.InfoResponse;
import com.yorel.muxon.api.dto.LicenseView;
import com.yorel.muxon.api.dto.ModuleView;
import com.yorel.muxon.common.Edition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InfoService {

  private final CapabilityRegistry capabilityRegistry;
  private final ModuleRegistry moduleRegistry;
  private final Edition edition;
  private final String productName;
  private final String version;
  private final LicenseCapabilityProvider licensedCapabilityProvider;

  public InfoService(
      CapabilityRegistry capabilityRegistry,
      ModuleRegistry moduleRegistry,
      LicenseCapabilityProvider licensedCapabilityProvider,
      @Value("${muxon.edition:CORE}") String editionValue,
      @Value("${muxon.product:Muxon}") String productName,
      @Value("${muxon.version:unknown}") String version) {
    this.capabilityRegistry = capabilityRegistry;
    this.moduleRegistry = moduleRegistry;
    this.licensedCapabilityProvider = licensedCapabilityProvider;
    this.edition = Edition.valueOf(editionValue.toUpperCase());
    this.productName = productName;
    this.version = version;
  }

  public InfoResponse getInfo() {
    Set<String> capabilities = capabilityRegistry.resolveCapabilities();

    List<ModuleView> moduleViews =
        moduleRegistry.getAllModules().stream()
            .map(
                m ->
                    new ModuleView(m.getId(), m.getDisplayName(), m.getCategory(), m.getMetadata()))
            .collect(Collectors.toList());

    LicenseView license = licensedCapabilityProvider.getLicenseView();

    // Extras is kept flexible for simple boolean flags used directly by the UI.
    Map<String, Object> extras = Map.of();

    return new InfoResponse(
        productName,
        edition.name().toLowerCase(),
        version,
        license,
        capabilities.stream().sorted().toList(),
        moduleViews,
        extras);
  }
}
