package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.VmStatus;
import com.onetattva.infron.db.model.VmPowerState;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Infron Libvirt Provider implementation.
 * Manages VM operations on KVM/QEMU hypervisors via libvirt API.
 *
 * <p>Libvirt does not have a native cluster concept. When a Libvirt provider is added,
 * a NodeCluster with a single Node is automatically created.</p>
 */
public class LibvirtVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtVmProvider.class);

    private final ProviderEntity provider;
    private final NodeEntity node;
    private final LibvirtConnectionManager connectionManager;

    /**
     * Creates a new Libvirt VM provider instance.
     *
     * @param provider The provider entity containing configuration
     * @param node The node entity representing hypervisor
     */
    public LibvirtVmProvider(ProviderEntity provider, NodeEntity node) {
        this.provider = provider;
        this.node = node;
        this.connectionManager = new LibvirtConnectionManager(
                provider.getEndpoint(),
                provider.getCredentials()
        );
    }

    @Override
    public String id() {
        return "libvirt-" + provider.getId();
    }

    @Override
    public String description() {
        return "Infron Libvirt provider for " + provider.getName();
    }

    @Override
    public CompletableFuture<VmCreationResult> createVm(VmCreationRequest request) {
        logger.info("Creating VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                // Convert VmSpec to Libvirt XML
                String domainXml = LibvirtXmlBuilder.buildDomainXml(request);

                logger.debug("Libvirt domain XML for VM {}: {}", request.getVmId(), domainXml);

                // Define and start domain
                Domain domain = connection.domainDefineXML(domainXml);
                domain.create();

                // Get VM info
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully created VM {} on Libvirt provider {} with external ID: {}",
                        request.getVmId(), provider.getName(), domain.getName());

                return VmCreationResult.success(domain.getName(), vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to create VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmCreationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to create VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .details(Map.of(
                                        "libvirtError", e.getError().toString(),
                                        "libvirtMessage", e.getMessage()
                                ))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Deleting VM {} from Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmDeletionResult.failure("VM not found");
                }

                String domainName = domain.getName();

                // Stop if running
                if (domain.isActive() == 1) {
                    logger.debug("Stopping VM {} before deletion", domainName);
                    domain.destroy();
                }

                // Undefine (remove)
                domain.undefine();

                logger.info("Successfully deleted VM {} from Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmDeletionResult.success();

            } catch (LibvirtException e) {
                logger.error("Failed to delete VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmDeletionResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to delete VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> startVm(VmOperationRequest request) {
        logger.info("Starting VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 1) {
                    logger.debug("VM {} is already running", request.getVmId());
                    return VmOperationResult.failure("VM is already running");
                }

                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully started VM {} on Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to start VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to start VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Stopping VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is already stopped", request.getVmId());
                    return VmOperationResult.failure("VM is already stopped");
                }

                domain.destroy();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully stopped VM {} on Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to stop VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to stop VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Restarting VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmOperationResult.failure("VM not found");
                }

                domain.reboot(0);
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully restarted VM {} on Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to restart VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to restart VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Suspending VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is not running", request.getVmId());
                    return VmOperationResult.failure("VM is not running");
                }

                // Use managedSave to preserve state to disk
                domain.managedSave();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully suspended VM {} on Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to suspend VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to suspend VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Resuming VM {} on Libvirt provider {}", request.getVmId(), provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByUUIDString(
                        request.getVmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.getVmId(), provider.getName());
                    return VmOperationResult.failure("VM not found");
                }

                // Remove managed save state and start
                domain.managedSaveRemove();
                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully resumed VM {} on Libvirt provider {}",
                        request.getVmId(), provider.getName());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to resume VM {} on Libvirt provider {}: {}",
                        request.getVmId(), provider.getName(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to resume VM: " + e.getMessage())
                                .providerErrorCode(String.valueOf(e.getError()))
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Getting VM info for {} on Libvirt provider {}", externalVmId, provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                Domain domain = connection.domainLookupByName(externalVmId);

                if (domain == null) {
                    logger.debug("VM {} not found on Libvirt provider {}", externalVmId, provider.getName());
                    return Optional.empty();
                }

                return Optional.of(getVmInfoFromDomain(domain));

            } catch (LibvirtException e) {
                logger.error("Failed to get VM info for {} on Libvirt provider {}: {}",
                        externalVmId, provider.getName(), e.getMessage(), e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<List<VmInfo>> listVms(VmListRequest request) {
        logger.debug("Listing VMs on Libvirt provider {}", provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                String[] activeDomainIds = connection.listDomains();
                String[] inactiveDomainIds = connection.listDefinedDomains();

                List<VmInfo> vmList = new ArrayList<>();

                // Active domains
                for (String domainId : activeDomainIds) {
                    try {
                        Domain domain = connection.domainLookupByUUIDString(domainId);
                        vmList.add(getVmInfoFromDomain(domain));
                    } catch (LibvirtException e) {
                        logger.warn("Failed to get info for active domain {}: {}", domainId, e.getMessage());
                    }
                }

                // Inactive domains
                for (String domainId : inactiveDomainIds) {
                    try {
                        Domain domain = connection.domainLookupByName(domainId);
                        vmList.add(getVmInfoFromDomain(domain));
                    } catch (LibvirtException e) {
                        logger.warn("Failed to get info for inactive domain {}: {}", domainId, e.getMessage());
                    }
                }

                logger.info("Found {} VMs on Libvirt provider {}", vmList.size(), provider.getName());
                return vmList;

            } catch (LibvirtException e) {
                logger.error("Failed to list VMs on Libvirt provider {}: {}",
                        provider.getName(), e.getMessage(), e);
                return List.of();
            }
        });
    }

    @Override
    public CompletableFuture<ProviderCapabilities> getCapabilities() {
        logger.debug("Getting capabilities for Libvirt provider {}", provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            try (Connect connection = connectionManager.getConnection()) {
                String capabilitiesXml = connection.getCapabilities();
                LibvirtCapabilities caps = LibvirtCapabilitiesParser.parse(capabilitiesXml);

                logger.debug("Libvirt provider {} capabilities: {}", provider.getName(), caps);

                return ProviderCapabilities.builder()
                        .supportedCpuTypes(caps.getSupportedCpuTypes())
                        .supportedStorageClasses(caps.getSupportedDiskTypes())
                        .supportedNetworkTypes(caps.getSupportedNetworkModels())
                        .supportedOsTypes(List.of("linux", "windows", "bsd"))
                        .resourceLimits(ResourceLimits.builder()
                                .maxCpuCores(caps.getMaxCpuCores())
                                .maxMemoryMb(caps.getMaxMemoryMb())
                                .maxStorageGb(caps.getMaxStorageGb())
                                .maxVms(caps.getMaxVms())
                                .build())
                        .features(Map.of(
                                "virtio", caps.supportsVirtio(),
                                "nestedVirtualization", caps.supportsNestedVirtualization(),
                                "liveMigration", caps.supportsLiveMigration()
                        ))
                        .build();

            } catch (LibvirtException e) {
                logger.error("Failed to get capabilities for Libvirt provider {}: {}",
                        provider.getName(), e.getMessage(), e);

                // Return empty capabilities on error
                return ProviderCapabilities.builder()
                        .supportedCpuTypes(List.of())
                        .supportedStorageClasses(List.of())
                        .supportedNetworkTypes(List.of())
                        .supportedOsTypes(List.of())
                        .resourceLimits(ResourceLimits.builder().build())
                        .features(Map.of())
                        .build();
            }
        });
    }

    @Override
    public CompletableFuture<ValidationResult> validateVmSpec(VmSpec spec) {
        logger.debug("Validating VM spec for Libvirt provider {}", provider.getName());

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();

            // Validate CPU
            if (spec.getCpu().getCores() < 1 || spec.getCpu().getCores() > 256) {
                errors.add("CPU cores must be between 1 and 256");
            }

            // Validate memory
            if (spec.getMemory().getSizeMb() < 512 || spec.getMemory().getSizeMb() > 1048576) {
                errors.add("Memory must be between 512MB and 1TB");
            }

            // Validate storage
            if (spec.getStorage().isEmpty()) {
                errors.add("At least one storage device is required");
            }

            for (int i = 0; i < spec.getStorage().size(); i++) {
                var storage = spec.getStorage().get(i);
                if (storage.getSizeGb() < 1 || storage.getSizeGb() > 10240) {
                    errors.add("Storage device " + i + " size must be between 1GB and 10TB");
                }
            }

            // Validate network
            if (spec.getNetwork().isEmpty()) {
                errors.add("At least one network interface is required");
            }

            boolean valid = errors.isEmpty();
            if (valid) {
                logger.debug("VM spec validation passed for Libvirt provider {}", provider.getName());
            } else {
                logger.debug("VM spec validation failed for Libvirt provider {}: {}",
                        provider.getName(), errors);
            }

            return ValidationResult.builder()
                    .valid(valid)
                    .errors(errors)
                    .build();
        });
    }

    /**
     * Converts Libvirt Domain to VmInfo.
     *
     * @param domain The Libvirt domain
     * @return VmInfo object
     * @throws LibvirtException if domain info cannot be retrieved
     */
    private VmInfo getVmInfoFromDomain(Domain domain) throws LibvirtException {
        DomainInfo info = domain.getInfo();

        // Map Libvirt state to VmStatus and VmPowerState
        VmStatus status = mapLibvirtStateToVmStatus(info.state);
        VmPowerState powerState = mapLibvirtStateToPowerState(info.state);

        // Get IP addresses from domain interfaces
        List<String> ipAddresses = getDomainIpAddresses(domain);

        // Get hostname from domain metadata
        String hostname = domain.getMetadata() != null
                ? extractHostnameFromMetadata(domain.getMetadata())
                : domain.getName();

        return new VmInfo(
                domain.getName(),
                status,
                powerState,
                ipAddresses,
                hostname,
                null, // resourceUsage - not available from basic domain info
                Map.of(
                        "libvirtDomainId", String.valueOf(info.id),
                        "libvirtState", String.valueOf(info.state),
                        "libvirtCpuTime", String.valueOf(info.cpuTime),
                        "libvirtMaxMem", String.valueOf(info.maxMem),
                        "libvirtMemory", String.valueOf(info.memory)
                ),
                Instant.now()
        );
    }

    /**
     * Maps Libvirt domain state to VmStatus.
     *
     * @param state The Libvirt domain state bitmask
     * @return Corresponding VmStatus
     */
    private VmStatus mapLibvirtStateToVmStatus(int state) {
        // Libvirt state is a bitmask:
        // VIR_DOMAIN_NOSTATE = 0
        // VIR_DOMAIN_RUNNING = 1
        // VIR_DOMAIN_BLOCKED = 2
        // VIR_DOMAIN_PAUSED = 3
        // VIR_DOMAIN_SHUTDOWN = 4
        // VIR_DOMAIN_SHUTOFF = 5
        // VIR_DOMAIN_CRASHED = 6
        // VIR_DOMAIN_PMSUSPENDED = 7

        if ((state & DomainState.VIR_DOMAIN_RUNNING) != 0) {
            return VmStatus.ACTIVE;
        } else if ((state & DomainState.VIR_DOMAIN_PAUSED) != 0) {
            return VmStatus.SUSPENDED;
        } else if ((state & DomainState.VIR_DOMAIN_SHUTOFF) != 0) {
            return VmStatus.STOPPED;
        } else if ((state & DomainState.VIR_DOMAIN_CRASHED) != 0) {
            return VmStatus.ERROR;
        }

        return VmStatus.UNKNOWN;
    }

    /**
     * Maps Libvirt domain state to VmPowerState.
     *
     * @param state The Libvirt domain state bitmask
     * @return Corresponding VmPowerState
     */
    private VmPowerState mapLibvirtStateToPowerState(int state) {
        if ((state & DomainState.VIR_DOMAIN_RUNNING) != 0) {
            return VmPowerState.ON;
        } else if ((state & DomainState.VIR_DOMAIN_PAUSED) != 0) {
            return VmPowerState.SUSPENDED;
        } else {
            return VmPowerState.OFF;
        }
    }

    /**
     * Extracts IP addresses from domain network interfaces.
     *
     * @param domain The Libvirt domain
     * @return List of IP addresses
     * @throws LibvirtException if interface info cannot be retrieved
     */
    private List<String> getDomainIpAddresses(Domain domain) throws LibvirtException {
        List<String> ipAddresses = new ArrayList<>();

        try {
            DomainInterface[] interfaces = domain.interfaceAddresses(Connect.VIR_DOMAIN_INTERFACE_ADDRESSES_SRC_LEASE);

            for (DomainInterface iface : interfaces) {
                for (DomainInterfaceIPAddress addr : iface.getAddrs()) {
                    if (addr.getType() == 0) { // IPv4
                        ipAddresses.add(addr.getAddr());
                    }
                }
            }
        } catch (LibvirtException e) {
            // Interface addresses may not be available for all VMs
            logger.debug("Could not get interface addresses for VM {}: {}",
                    domain.getName(), e.getMessage());
        }

        return ipAddresses;
    }

    /**
     * Extracts hostname from domain metadata XML.
     *
     * @param metadata The metadata XML string
     * @return Extracted hostname or null
     */
    private String extractHostnameFromMetadata(String metadata) {
        // Simple parsing - in production, use proper XML parser
        if (metadata != null && metadata.contains("<hostname>")) {
            int start = metadata.indexOf("<hostname>") + 10;
            int end = metadata.indexOf("</hostname>");
            if (start > 0 && end > start) {
                return metadata.substring(start, end);
            }
        }
        return null;
    }

    /**
     * Closes all open connections to Libvirt.
     */
    public void close() {
        connectionManager.closeAll();
    }
}
