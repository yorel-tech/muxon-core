package com.krito.muxon.core.providers;

import com.krito.muxon.api.model.VmPowerState;
import com.krito.muxon.api.model.VmStatus;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Mock VM provider for testing and development.
 * Simulates VM operations without actual infrastructure.
 */
@ApplicationScoped
public class MockVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockVmProvider.class);
    
    private final Map<String, MockVm> vms = new HashMap<>();

    @Override
    public String id() {
        return "mock";
    }

    @Override
    public String description() {
        return "Mock VM provider for testing and development";
    }

    @Override
    public CompletableFuture<VmCreationResult> createVm(VmCreationRequest request) {
        logger.info("Mock: Creating VM {} with spec", request.vmId(), request.spec());
        
        // Create mock VM
        MockVm vm = MockVm.builder()
                .id(request.vmId())
                .externalId("mock-" + request.vmId().toString().substring(0, 8))
                .status(VmStatus.ACTIVE)
                .powerState(VmPowerState.ON)
                .ipAddresses(List.of("192.168.1." + (100 + vms.size() % 255)))
                .hostname("mock-vm-" + request.vmId().toString().substring(0, 8))
                .createdAt(Instant.now())
                .spec(request.spec())
                .build();
        
        vms.put(request.vmId().toString(), vm);
        
        // Simulate async creation delay
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmCreationResult.success(
                    vm.externalId(),
                    vm.toVmInfo()
            );
        });
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Mock: Deleting VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmDeletionResult.failure("VM not found: " + request.vmId())
            );
        }
        
        vms.remove(request.vmId().toString());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmDeletionResult.success();
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> startVm(VmOperationRequest request) {
        logger.info("Mock: Starting VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.vmId())
            );
        }
        
        if (vm.powerState() == VmPowerState.ON) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already running")
            );
        }
        
        // Update vm state - records are immutable, so we need to rebuild
        MockVm updatedVm = new MockVm(
                vm.id(),
                vm.externalId(),
                vm.status(),
                VmPowerState.ON,
                vm.ipAddresses(),
                vm.hostname(),
                vm.metadata(),
                vm.createdAt(),
                Instant.now(),
                vm.spec()
        );
        vms.put(request.vmId().toString(), updatedVm);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(vm.toVmInfo());
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Mock: Stopping VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.vmId())
            );
        }
        
        if (vm.powerState() == VmPowerState.OFF) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already stopped")
            );
        }
        
        // Update vm state - records are immutable, so we need to rebuild
        MockVm updatedVm = new MockVm(
                vm.id(),
                vm.externalId(),
                vm.status(),
                VmPowerState.OFF,
                vm.ipAddresses(),
                vm.hostname(),
                vm.metadata(),
                vm.createdAt(),
                Instant.now(),
                vm.spec()
        );
        vms.put(request.vmId().toString(), updatedVm);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(vm.toVmInfo());
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Mock: Restarting VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.vmId())
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(750); // Simulate restart taking longer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Update vm state - records are immutable, so we need to rebuild
            MockVm updatedVm = new MockVm(
                    vm.id(),
                    vm.externalId(),
                    vm.status(),
                    VmPowerState.ON,
                    vm.ipAddresses(),
                    vm.hostname(),
                    vm.metadata(),
                    vm.createdAt(),
                    Instant.now(),
                    vm.spec()
            );
            vms.put(request.vmId().toString(), updatedVm);
            
            return VmOperationResult.success(vm.toVmInfo());
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Mock: Suspending VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.vmId())
            );
        }
        
        if (vm.powerState() == VmPowerState.SUSPENDED) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already suspended")
            );
        }
        
        // Update vm state - records are immutable, so we need to rebuild
        MockVm updatedVm = new MockVm(
                vm.id(),
                vm.externalId(),
                vm.status(),
                VmPowerState.SUSPENDED,
                vm.ipAddresses(),
                vm.hostname(),
                vm.metadata(),
                vm.createdAt(),
                Instant.now(),
                vm.spec()
        );
        vms.put(request.vmId().toString(), updatedVm);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(vm.toVmInfo());
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Mock: Resuming VM {}", request.vmId());
        
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.vmId())
            );
        }
        
        if (vm.powerState() != VmPowerState.SUSPENDED) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is not suspended")
            );
        }
        
        // Update vm state - records are immutable, so we need to rebuild
        MockVm updatedVm = new MockVm(
                vm.id(),
                vm.externalId(),
                vm.status(),
                VmPowerState.ON,
                vm.ipAddresses(),
                vm.hostname(),
                vm.metadata(),
                vm.createdAt(),
                Instant.now(),
                vm.spec()
        );
        vms.put(request.vmId().toString(), updatedVm);
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(vm.toVmInfo());
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Mock: Getting VM info for {}", externalVmId);
        
        for (MockVm vm : vms.values()) {
            if (vm.externalId().equals(externalVmId)) {
                return CompletableFuture.completedFuture(Optional.of(vm.toVmInfo()));
            }
        }
        
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public CompletableFuture<List<VmInfo>> listVms(VmListRequest request) {
        logger.debug("Mock: Listing VMs");
        
        return CompletableFuture.completedFuture(
                vms.values().stream()
                        .map(MockVm::toVmInfo)
                        .toList()
        );
    }

    @Override
    public CompletableFuture<ProviderCapabilities> getCapabilities() {
        logger.debug("Mock: Getting provider capabilities");
        
        ProviderCapabilities capabilities = ProviderCapabilities.builder()
                .supportedCpuTypes(List.of("EMULATED", "HOST_PASSTHROUGH"))
                .supportedStorageClasses(List.of("ssd", "hdd", "nvme"))
                .supportedNetworkTypes(List.of("primary", "secondary"))
                .supportedOsTypes(List.of("linux", "windows", "bsd"))
                .resourceLimits(ResourceLimits.builder()
                        .maxCpuCores(128)
                        .maxMemoryMb(1048576)
                        .maxStorageGb(10240)
                        .maxVms(1000)
                        .build())
                .build();
        
        return CompletableFuture.completedFuture(capabilities);
    }

    @Override
    public CompletableFuture<ValidationResult> validateVmSpec(String spec) {
        logger.debug("Mock: Validating VM spec");
        
        // Simple validation - since spec is now a String, we'll do basic checks
        List<String> errors = new ArrayList<>();
        
        if (spec == null || spec.isEmpty()) {
            errors.add("VM spec cannot be null or empty");
        } else if (spec.length() > 100000) {
            errors.add("VM spec is too large (max 100KB)");
        }
        
        ValidationResult result = ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
        
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<VmOperationResult> attachIso(VmIsoAttachProviderRequest request) {
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(VmOperationResult.failure("VM not found"));
        }
        return CompletableFuture.completedFuture(VmOperationResult.success(vm.toVmInfo()));
    }

    @Override
    public CompletableFuture<VmOperationResult> detachIso(VmIsoDetachProviderRequest request) {
        MockVm vm = vms.get(request.vmId().toString());
        if (vm == null) {
            return CompletableFuture.completedFuture(VmOperationResult.failure("VM not found"));
        }
        return CompletableFuture.completedFuture(VmOperationResult.success(vm.toVmInfo()));
    }

    @Override
    public CompletableFuture<VmTemplateExportResult> cloneVmAsTemplate(VmTemplateExportRequest request) {
        return CompletableFuture.completedFuture(
                VmTemplateExportResult.success(
                        "/mock/" + request.destinationRelativePath(),
                        1024L,
                        Map.of("mock", "true")));
    }

    @Override
    public CompletableFuture<VmConsoleConnectionInfo> getConsoleConnection(VmConsoleRequest request) {
        // Deterministic loopback target for integration tests; no real VNC listener unless provided externally.
        return CompletableFuture.completedFuture(
                new VmConsoleConnectionInfo(VmConsoleType.VNC, "127.0.0.1", 5900, "mock", false, null));
    }

    /**
     * Clear all mock data (for testing)
     */
    public void clear() {
        vms.clear();
        logger.info("Cleared mock provider data");
    }
}
