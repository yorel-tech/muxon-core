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
package com.yorel.muxon.providers;

import com.yorel.muxon.api.model.ProviderType;
import java.util.Map;

/**
 * Generic provider-SDK interface for provider-level APIs (connection testing, capabilities, etc.).
 *
 * <p>Implementations live in provider modules (proxmox/libvirt/etc.) so core services do not depend
 * on provider SDK libraries.
 */
public interface ProviderSdk {

  /** The provider type this SDK supports. */
  ProviderType providerType();

  /**
   * Test connectivity/auth against the provider referenced by {@link ProviderConnectionInfo}.
   *
   * @param connectionInfo endpoint/credentials/connectionConfig for the provider
   * @return test outcome including optional capabilities
   */
  ProviderSdkConnectionTestResult testConnection(ProviderConnectionInfo connectionInfo);

  /**
   * Fetch provider capabilities (or an approximation).
   *
   * @param connectionInfo endpoint/credentials/connectionConfig for the provider
   * @return capabilities as a key/value map persisted by core-services
   */
  Map<String, String> getCapabilities(ProviderConnectionInfo connectionInfo);
}
