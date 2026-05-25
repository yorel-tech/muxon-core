package com.sal.muxon.providers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Core provider interface for VM orchestration.
 * This SPI defines the contract between orchestrator and infrastructure providers.
 */
public interface VmProvider {

    /**
     * Get the unique identifier for this provider
     */
    String id();

    /**
     * Get a human-readable description of this provider
     */
    String description();

    /**
     * Create a new VM instance
     *
     * @param request VM creation request with spec and placement hints
     * @return CompletableFuture with creation result
     */
    CompletableFuture<VmCreationResult> createVm(VmCreationRequest request);

    /**
     * Delete an existing VM
     *
     * @param request VM deletion request
     * @return CompletableFuture with deletion result
     */
    CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request);

    /**
     * Start a stopped VM
     *
     * @param request VM operation request
     * @return CompletableFuture with operation result
     */
    CompletableFuture<VmOperationResult> startVm(VmOperationRequest request);

    /**
     * Stop a running VM
     *
     * @param request VM operation request
     * @return CompletableFuture with operation result
     */
    CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request);

    /**
     * Restart a VM
     *
     * @param request VM operation request
     * @return CompletableFuture with operation result
     */
    CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request);

    /**
     * Suspend a VM (preserve state to disk)
     *
     * @param request VM operation request
     * @return CompletableFuture with operation result
     */
    CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request);

    /**
     * Resume a suspended VM
     *
     * @param request VM operation request
     * @return CompletableFuture with operation result
     */
    CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request);

    /**
     * Get VM status and details
     *
     * @param externalVmId Provider's VM ID
     * @return CompletableFuture with optional VM info
     */
    CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId);

    /**
     * List all VMs managed by this provider
     *
     * @param request VM list request
     * @return CompletableFuture with list of VM info
     */
    CompletableFuture<List<VmInfo>> listVms(VmListRequest request);

    /**
     * Get provider capabilities and available resources
     *
     * @return CompletableFuture with provider capabilities
     */
    CompletableFuture<ProviderCapabilities> getCapabilities();

    /**
     * Validate VM specification against provider capabilities
     *
     * @param spec VM specification to validate
     * @return CompletableFuture with validation result
     */
    CompletableFuture<ValidationResult> validateVmSpec(String spec);

    /**
     * Attach an ISO (file path on provider storage) to a running or defined VM.
     */
    CompletableFuture<VmOperationResult> attachIso(VmIsoAttachProviderRequest request);

    /**
     * Detach a CD-ROM device by guest target name (e.g. sdc).
     */
    CompletableFuture<VmOperationResult> detachIso(VmIsoDetachProviderRequest request);

    /**
     * Clone VM storage to a template file at the given provider-relative path.
     */
    CompletableFuture<VmTemplateExportResult> cloneVmAsTemplate(VmTemplateExportRequest request);

    /**
     * Resolve hypervisor console endpoint for a running VM (VNC/SPICE).
     */
    default CompletableFuture<VmConsoleConnectionInfo> getConsoleConnection(VmConsoleRequest request) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Console is not supported for this provider"));
    }

    /**
     * Query the QEMU guest agent channel for the VM's current IP addresses, hostname,
     * and (Linux only) the cloud-init status JSON.
     *
     * <p>Returns {@link GuestAgentInfo#unreachable()} when the guest agent has not yet started
     * rather than throwing, so callers can poll until the agent is available.
     *
     * @param externalVmId Provider-assigned VM ID.
     */
    default CompletableFuture<GuestAgentInfo> queryGuestAgent(String externalVmId) {
        return CompletableFuture.completedFuture(GuestAgentInfo.unreachable());
    }

    /**
     * Detach and eject the customization seed CD-ROM from the VM, then delete the backing ISO file.
     * Called once the guest customization phase reaches {@code COMPLETE} or {@code FAILED}.
     *
     * @param externalVmId Provider-assigned VM ID.
     * @param seedIsoPath  Absolute path of the seed ISO to delete after detach.
     */
    default CompletableFuture<VmOperationResult> detachCustomizationSeed(String externalVmId, String seedIsoPath) {
        return CompletableFuture.completedFuture(
                VmOperationResult.failure("Seed detach not supported by this provider"));
    }
}
