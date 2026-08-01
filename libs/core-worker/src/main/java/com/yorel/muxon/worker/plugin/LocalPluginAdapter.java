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

import com.yorel.muxon.providers.VmCreationRequest;
import com.yorel.muxon.providers.VmCreationResult;
import com.yorel.muxon.providers.VmDeletionRequest;
import com.yorel.muxon.providers.VmDeletionResult;
import com.yorel.muxon.providers.VmOperationRequest;
import com.yorel.muxon.providers.VmOperationResult;
import com.yorel.muxon.providers.VmProvider;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges an in-process {@link VmProvider} (hypervisor driver) to the {@code
 * RuntimeCapabilityService} interface contract, allowing BUILTIN plugins (libvirt, proxmox) to
 * participate in health checking without going through gRPC.
 *
 * <p>This is NOT a gRPC endpoint. It is an in-process adapter used only by {@code
 * BuiltinPluginBootstrapper} so that the plugin framework can reference built-in providers as
 * BUILTIN-source plugins while still routing all actual VM operations through {@code
 * TenantAwareVmProviderRegistry}.
 */
public class LocalPluginAdapter {

  private final VmProvider delegate;
  private final String pluginName;

  public LocalPluginAdapter(VmProvider delegate, String pluginName) {
    this.delegate = delegate;
    this.pluginName = pluginName;
  }

  public String getPluginName() {
    return pluginName;
  }

  public VmProvider getDelegate() {
    return delegate;
  }

  // ── Delegating operations ────────────────────────────────────────────────

  public CompletableFuture<VmCreationResult> provisionRuntime(VmCreationRequest request) {
    return delegate.createVm(request);
  }

  public CompletableFuture<VmDeletionResult> deprovisionRuntime(VmDeletionRequest request) {
    return delegate.deleteVm(request);
  }

  public CompletableFuture<VmOperationResult> getRuntimeStatus(VmOperationRequest request) {
    return delegate.startVm(request);
  }

  public boolean isHealthy() {
    try {
      String id = delegate.id();
      return id != null && !id.isBlank();
    } catch (Exception e) {
      return false;
    }
  }
}
