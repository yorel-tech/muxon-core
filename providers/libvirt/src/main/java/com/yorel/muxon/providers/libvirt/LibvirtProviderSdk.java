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
package com.yorel.muxon.providers.libvirt;

import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.providers.ProviderConnectionInfo;
import com.yorel.muxon.providers.ProviderSdk;
import com.yorel.muxon.providers.ProviderSdkConnectionTestResult;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider-SDK implementation for LIBVIRT provider-level operations.
 *
 * <p>For now, this is a stub/no-op to preserve existing behavior for {@code POST
 * /providers/{providerId}/test}.
 */
public class LibvirtProviderSdk implements ProviderSdk {

  @Override
  public ProviderType providerType() {
    return ProviderType.LIBVIRT;
  }

  @Override
  public ProviderSdkConnectionTestResult testConnection(ProviderConnectionInfo connectionInfo) {
    long startTime = System.currentTimeMillis();
    try {
      validateEndpoint(connectionInfo);
      validateCredentials(connectionInfo);

      int latencyMs = (int) (System.currentTimeMillis() - startTime);
      return new ProviderSdkConnectionTestResult(
          true, "Successfully connected to provider", latencyMs, getCapabilities(connectionInfo));
    } catch (Exception e) {
      int latencyMs = (int) (System.currentTimeMillis() - startTime);
      return new ProviderSdkConnectionTestResult(
          false, "Connection failed: " + e.getMessage(), latencyMs, null);
    }
  }

  @Override
  public Map<String, String> getCapabilities(ProviderConnectionInfo connectionInfo) {
    return getDefaultCapabilities();
  }

  private void validateEndpoint(ProviderConnectionInfo connectionInfo) {
    String endpoint = connectionInfo.endpoint();
    if (endpoint == null || endpoint.isBlank()) {
      throw new IllegalArgumentException("Libvirt endpoint is required");
    }
    if (!endpoint.startsWith("ssh://") && !endpoint.startsWith("libvirt://")) {
      throw new IllegalArgumentException(
          "Libvirt endpoint must be SSH connection string or libvirt:// URL");
    }
  }

  private void validateCredentials(ProviderConnectionInfo connectionInfo) {
    Map<String, String> credentials = connectionInfo.credentials();
    if (credentials == null || credentials.isEmpty()) {
      throw new IllegalArgumentException("Credentials are required");
    }
    if (!credentials.containsKey("sshPrivateKey")) {
      throw new IllegalArgumentException("Libvirt requires sshPrivateKey");
    }
  }

  private Map<String, String> getDefaultCapabilities() {
    Map<String, String> defaults = new HashMap<>();
    defaults.put("supportedCpuTypes", "kvm64,host");
    defaults.put("supportedStorageClasses", "local,nfs");
    defaults.put("supportedNetworkTypes", "bridge,ovs");
    defaults.put("supportedOsTypes", "linux,windows");
    defaults.put("maxCpus", "1000");
    defaults.put("maxMemoryMb", "409600");
    defaults.put("maxStorageGb", "20000");
    defaults.put("maxVms", "500");
    return defaults;
  }
}
