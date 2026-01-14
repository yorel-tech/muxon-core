package com.onetattva.infron.core.providers;

import com.onetattva.infron.core.providers.VmCreationRequest;
import com.onetattva.infron.core.providers.VmCreationResult;
import com.onetattva.infron.core.providers.VmDeletionRequest;
import com.onetattva.infron.core.providers.VmDeletionResult;
import com.onetattva.infron.core.providers.VmInfo;
import com.onetattva.infron.core.providers.VmListRequest;
import com.onetattva.infron.core.providers.VmOperationRequest;
import com.onetattva.infron.core.providers.VmOperationResult;
import com.onetattva.infron.core.providers.ProviderCapabilities;
import com.onetattva.infron.core.providers.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Mock VM provider for testing and development.
 * Simulates VM operations without actual infrastructure.
 */
@Component
public class MockVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(MockVmProvider.class);
    
    private final Map<String, MockVm> vms = new HashMap<>();
    private final Map<String, MockVmOperation> operations = new HashMap<>();

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
        logger.info("Mock: Creating VM {} with spec", request.getVmId(), request.getSpec());
        
        // Create mock VM
        MockVm vm = MockVm.builder()
                .id(request.getVmId())
                .externalId("mock-" + request.getVmId().toString().substring(0, 8))
                .status(VmStatus.ACTIVE)
                .powerState(VmPowerState.ON)
                .ipAddresses(List.of("192.168.1." + (100 + vms.size() % 255)))
                .hostname("mock-vm-" + request.getVmId().toString().substring(0, 8))
                .createdAt(Instant.now())
                .spec(request.getSpec())
                .build();
        
        vms.put(request.getVmId(), vm);
        
        // Simulate async creation delay
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmCreationResult.success(
                    vm.getExternalId(),
                    VmInfo.fromMock(vm)
            );
        });
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Mock: Deleting VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmDeletionResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        vms.remove(request.getVmId());
        
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
        logger.info("Mock: Starting VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        if (vm.getPowerState() == VmPowerState.ON) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already running")
            );
        }
        
        vm.setPowerState(VmPowerState.ON);
        vm.setUpdatedAt(Instant.now());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(VmInfo.fromMock(vm));
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Mock: Stopping VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        if (vm.getPowerState() == VmPowerState.OFF) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already stopped")
            );
        }
        
        vm.setPowerState(VmPowerState.OFF);
        vm.setUpdatedAt(Instant.now());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(VmInfo.fromMock(vm));
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Mock: Restarting VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(750); // Simulate restart taking longer
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            vm.setPowerState(VmPowerState.ON);
            vm.setUpdatedAt(Instant.now());
            
            return VmOperationResult.success(VmInfo.fromMock(vm));
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Mock: Suspending VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        if (vm.getPowerState() == VmPowerState.SUSPENDED) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is already suspended")
            );
        }
        
        vm.setPowerState(VmPowerState.SUSPENDED);
        vm.setUpdatedAt(Instant.now());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(VmInfo.fromMock(vm));
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Mock: Resuming VM {}", request.getVmId());
        
        MockVm vm = vms.get(request.getVmId());
        if (vm == null) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM not found: " + request.getVmId())
            );
        }
        
        if (vm.getPowerState() != VmPowerState.SUSPENDED) {
            return CompletableFuture.completedFuture(
                    VmOperationResult.failure("VM is not suspended")
            );
        }
        
        vm.setPowerState(VmPowerState.ON);
        vm.setUpdatedAt(Instant.now());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return VmOperationResult.success(VmInfo.fromMock(vm));
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Mock: Getting VM info for {}", externalVmId);
        
        for (MockVm vm : vms.values()) {
            if (vm.getExternalId().equals(externalVmId)) {
                return CompletableFuture.completedFuture(Optional.of(VmInfo.fromMock(vm)));
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
    public CompletableFuture<ValidationResult> validateVmSpec(VmSpec spec) {
        logger.debug("Mock: Validating VM spec");
        
        // Simple validation
        List<String> errors = new ArrayList<>();
        
        if (spec.getCpu().getCores() < 1 || spec.getCpu().getCores() > 128) {
            errors.add("CPU cores must be between 1 and 128");
        }
        
        if (spec.getMemory().getSizeMb() < 512 || spec.getMemory().getSizeMb() > 1048576) {
            errors.add("Memory must be between 512MB and 1TB");
        }
        
        if (spec.getStorage().isEmpty()) {
            errors.add("At least one storage device is required");
        }
        
        ValidationResult result = ValidationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
        
        return CompletableFuture.completedFuture(result);
    }

    /**
     * Clear all mock data (for testing)
     */
    public void clear() {
        vms.clear();
        operations.clear();
        logger.info("Cleared mock provider data");
    }
}
