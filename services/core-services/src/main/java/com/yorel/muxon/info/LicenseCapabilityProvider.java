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

import com.yorel.muxon.api.dto.LicenseView;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Provides capabilities from the configured license. */
@Component
public class LicenseCapabilityProvider implements CapabilityProvider {

  private final LicenseView licenseView;

  public LicenseCapabilityProvider(
      @Value("${muxon.license.type:core}") String type,
      @Value("${muxon.license.expires:}") String expires) {
    Map<String, Object> limits = new HashMap<>();
    this.licenseView = new LicenseView(type, expires.isBlank() ? null : expires, limits);
  }

  public LicenseView getLicenseView() {
    return licenseView;
  }

  @Override
  public Set<String> getCapabilities() {
    Set<String> capabilities = new HashSet<>();
    if ("enterprise".equalsIgnoreCase(licenseView.getType())) {
      capabilities.add("backup.create");
      capabilities.add("backup.schedule");
      capabilities.add("backup.restore");
      capabilities.add("monitoring.metrics");
    }
    return capabilities;
  }
}
