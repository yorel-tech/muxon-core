package com.scal.muxon.worker.plugin;

import com.scal.muxon.providers.VmCreationRequest;
import com.scal.muxon.providers.VmCreationResult;
import com.scal.muxon.providers.VmDeletionRequest;
import com.scal.muxon.providers.VmDeletionResult;
import com.scal.muxon.providers.VmOperationRequest;
import com.scal.muxon.providers.VmOperationResult;
import com.scal.muxon.providers.VmProvider;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bridges an in-process {@link VmProvider} (hypervisor driver) to the
 * {@code RuntimeCapabilityService} interface contract, allowing BUILTIN plugins
 * (libvirt, proxmox) to participate in health checking without going through gRPC.
 *
 * <p>This is NOT a gRPC endpoint. It is an in-process adapter used only by
 * {@code BuiltinPluginBootstrapper} so that the plugin framework can reference
 * built-in providers as BUILTIN-source plugins while still routing all actual
 * VM operations through {@code TenantAwareVmProviderRegistry}.
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
