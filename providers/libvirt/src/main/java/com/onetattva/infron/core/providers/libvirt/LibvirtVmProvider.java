package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.core.providers.spec.NodeSpec;
import com.onetattva.infron.api.enums.VmStatus;
import com.onetattva.infron.api.enums.VmPowerState;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.libvirt.DomainInfo.DomainState.VIR_DOMAIN_RUNNING;

/**
 * Infron Libvirt Provider implementation.
 * Manages VM operations on KVM/QEMU hypervisors via libvirt API.
 *
 * <p>Libvirt does not have a native cluster concept. When a Libvirt provider is added,
 * a NodeCluster with a single Node is automatically created.</p>
 */
public class LibvirtVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtVmProvider.class);

    private final NodeSpec node;
    private final LibvirtConnectionManager connectionManager;

    /**
     * Creates a new Libvirt VM provider instance.
     *
     * @param node The node specification containing hypervisor configuration
     */
    public LibvirtVmProvider(NodeSpec node) {
        this.node = node;
        this.connectionManager = new LibvirtConnectionManager(
                node.endpoint(),
                node.credentials()
        );
    }

    @Override
    public String id() {
        return "libvirt-" + node.id();
    }

    @Override
    public String description() {
        return "Infron Libvirt provider for " + node.name();
    }

    @Override
    public CompletableFuture<VmCreationResult> createVm(VmCreationRequest request) {
        logger.info("Creating VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                // Convert VmSpec to Libvirt XML
                String domainXml = LibvirtXmlBuilder.buildDomainXml(request.vmId(), request.spec());

                logger.debug("Libvirt domain XML for VM {}: {}", request.vmId(), domainXml);

                // Define and start domain
                Domain domain = connection.domainDefineXML(domainXml);
                domain.create();

                // Get VM info
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully created VM {} on Libvirt provider {} with external ID: {}",
                        request.vmId(), node.name(), domain.getName());

                return VmCreationResult.success(domain.getName(), vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to create VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmCreationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to create VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .details(Map.of(
                                        "libvirtError", e.getError().toString(),
                                        "libvirtMessage", e.getMessage()
                                ))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Deleting VM {} from Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
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
                        request.vmId(), node.name());

                return VmDeletionResult.success();

            } catch (LibvirtException e) {
                logger.error("Failed to delete VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmDeletionResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to delete VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> startVm(VmOperationRequest request) {
        logger.info("Starting VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 1) {
                    logger.debug("VM {} is already running", request.vmId());
                    return VmOperationResult.failure("VM is already running");
                }

                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully started VM {} on Libvirt provider {}",
                        request.vmId(), node.name());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to start VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to start VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Stopping VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is already stopped", request.vmId());
                    return VmOperationResult.failure("VM is already stopped");
                }

                domain.destroy();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully stopped VM {} on Libvirt provider {}",
                        request.vmId(), node.name());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to stop VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to stop VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Restarting VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
                    return VmOperationResult.failure("VM not found");
                }

                domain.reboot(0);
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully restarted VM {} on Libvirt provider {}",
                        request.vmId(), node.name());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to restart VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to restart VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Suspending VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is not running", request.vmId());
                    return VmOperationResult.failure("VM is not running");
                }

                // Use managedSave to preserve state to disk
                domain.managedSave();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully suspended VM {} on Libvirt provider {}",
                        request.vmId(), node.name());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to suspend VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to suspend VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Resuming VM {} on Libvirt provider {}", request.vmId(), node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), node.name());
                    return VmOperationResult.failure("VM not found");
                }

                // Remove managed save state and start
                domain.managedSaveRemove();
                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully resumed VM {} on Libvirt provider {}",
                        request.vmId(), node.name());

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to resume VM {} on Libvirt provider {}: {}",
                        request.vmId(), node.name(), e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to resume VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Getting VM info for {} on Libvirt provider {}", externalVmId, node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                Domain domain = connection.domainLookupByName(externalVmId);

                if (domain == null) {
                    logger.debug("VM {} not found on Libvirt provider {}", externalVmId, node.name());
                    return Optional.empty();
                }

                return Optional.of(getVmInfoFromDomain(domain));

            } catch (LibvirtException e) {
                logger.error("Failed to get VM info for {} on Libvirt provider {}: {}",
                        externalVmId, node.name(), e.getMessage(), e);
                return Optional.empty();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<List<VmInfo>> listVms(VmListRequest request) {
        logger.debug("Listing VMs on Libvirt provider {}", node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                int[] activeDomainIds = connection.listDomains();
                String[] inactiveDomainIds = connection.listDefinedDomains();

                List<VmInfo> vmList = new ArrayList<>();

                // Active domains
                for (int domainId : activeDomainIds) {
                    try {
                        Domain domain = connection.domainLookupByUUIDString(String.valueOf(domainId));
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

                logger.info("Found {} VMs on Libvirt provider {}", vmList.size(), node.name());
                return vmList;

            } catch (LibvirtException e) {
                logger.error("Failed to list VMs on Libvirt provider {}: {}",
                        node.name(), e.getMessage(), e);
                return List.of();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<ProviderCapabilities> getCapabilities() {
        logger.debug("Getting capabilities for Libvirt provider {}", node.name());

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                connection = connectionManager.getConnection();
                String capabilitiesXml = connection.getCapabilities();
                LibvirtCapabilities caps = LibvirtCapabilitiesParser.parse(capabilitiesXml);

                logger.debug("Libvirt provider {} capabilities: {}", node.name(), caps);

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
                        node.name(), e.getMessage(), e);

                // Return empty capabilities on error
                return ProviderCapabilities.builder()
                        .supportedCpuTypes(List.of())
                        .supportedStorageClasses(List.of())
                        .supportedNetworkTypes(List.of())
                        .supportedOsTypes(List.of())
                        .resourceLimits(ResourceLimits.builder().build())
                        .features(Map.of())
                        .build();
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (LibvirtException e) {
                        logger.warn("Error closing connection: {}", e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<ValidationResult> validateVmSpec(String spec) {
        logger.debug("Validating VM spec for Libvirt provider {}", node.name());

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            
            // For now, just check if spec is a valid JSON string
            // In a real implementation, we would parse the JSON and validate the structure
            if (spec == null || spec.trim().isEmpty()) {
                errors.add("VM spec cannot be null or empty");
            } else {
                try {
                    // Simple JSON validation
                    if (!spec.trim().startsWith("{") || !spec.trim().endsWith("}")) {
                        errors.add("VM spec must be a valid JSON object");
                    }
                } catch (Exception e) {
                    errors.add("VM spec is not valid JSON: " + e.getMessage());
                }
            }

            boolean valid = errors.isEmpty();
            if (valid) {
                logger.debug("VM spec validation passed for Libvirt provider {}", node.name());
            } else {
                logger.debug("VM spec validation failed for Libvirt provider {}: {}",
                        node.name(), errors);
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
        String hostname = domain.getConnect().getHostName();

        return new VmInfo(
                domain.getName(),
                status,
                powerState,
                ipAddresses,
                hostname,
                null, // resourceUsage - not available from basic domain info
                Map.of(
                        "libvirtDomainId", domain.getID() > 0 ? String.valueOf(domain.getID()) : "",
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
    private VmStatus mapLibvirtStateToVmStatus(DomainInfo.DomainState state) {
        // Libvirt state is a bitmask:
        // VIR_DOMAIN_NOSTATE = 0
        // VIR_DOMAIN_RUNNING = 1
        // VIR_DOMAIN_BLOCKED = 2
        // VIR_DOMAIN_PAUSED = 3
        // VIR_DOMAIN_SHUTDOWN = 4
        // VIR_DOMAIN_SHUTOFF = 5
        // VIR_DOMAIN_CRASHED = 6
        // VIR_DOMAIN_PMSUSPENDED = 7
        return switch (state) {
            case VIR_DOMAIN_RUNNING -> VmStatus.ACTIVE;
            case VIR_DOMAIN_PAUSED -> VmStatus.SUSPENDED;
            case VIR_DOMAIN_SHUTOFF -> VmStatus.STOPPED;
            default -> VmStatus.ERROR;
        };
    }

    /**
     * Maps Libvirt domain state to VmPowerState.
     *
     * @param state The Libvirt domain state bitmask
     * @return Corresponding VmPowerState
     */
    private VmPowerState mapLibvirtStateToPowerState(DomainInfo.DomainState state) {
        return switch (state) {
            case VIR_DOMAIN_RUNNING -> VmPowerState.ON;
            case VIR_DOMAIN_PAUSED -> VmPowerState.SUSPENDED;
            default -> VmPowerState.OFF;
        };
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
            Collection<DomainInterface> interfaces = domain.interfaceAddresses(0, 0); // VIR_DOMAIN_INTERFACE_ADDRESSES_SRC_LEASE = 0

            for (DomainInterface iface : interfaces) {
                // Skip IP address extraction for now due to API differences
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
