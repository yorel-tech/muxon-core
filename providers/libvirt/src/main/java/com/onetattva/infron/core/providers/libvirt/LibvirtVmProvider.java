package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.repository.NodeRepository;
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
 * Manages VM operations on multiple KVM/QEMU hypervisors via libvirt API.
 *
 * <p>Unlike Proxmox which manages multiple nodes natively, Libvirt SDK manages
 * only one node at a time. This provider manages multiple Libvirt nodes and
 * implements placement logic to distribute VMs across nodes based on available resources.</p>
 */
public class LibvirtVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtVmProvider.class);

    private final UUID providerId;
    private final LibvirtMultiNodeConnectionManager multiNodeManager;
    private final LibvirtNodePlacementService placementService;
    private final Map<UUID, UUID> vmToNodeMapping = new HashMap<>();

    /**
     * Creates a new Libvirt VM provider instance.
     *
     * @param providerId The provider ID
     * @param nodeRepository The node repository for database access
     */
    public LibvirtVmProvider(UUID providerId, NodeRepository nodeRepository) {
        this.providerId = providerId;
        this.multiNodeManager = new LibvirtMultiNodeConnectionManager(providerId, nodeRepository);
        this.placementService = new LibvirtNodePlacementService();
    }

    @Override
    public String id() {
        return "libvirt-" + providerId;
    }

    @Override
    public String description() {
        List<NodeEntity> nodes = multiNodeManager.getAllNodes();
        return "Infron Libvirt provider managing " + nodes.size() + " node(s)";
    }

    @Override
    public CompletableFuture<VmCreationResult> createVm(VmCreationRequest request) {
        logger.info("Creating VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            Connect connection = null;
            try {
                List<NodeEntity> activeNodes = multiNodeManager.getActiveNodes();
                if (activeNodes.isEmpty()) {
                    return VmCreationResult.failure(
                            ProviderError.builder()
                                    .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                                    .message("No active nodes available for VM placement")
                                    .providerErrorCode("NO_ACTIVE_NODES")
                                    .retryable(true)
                                    .build()
                    );
                }

                Optional<NodeEntity> selectedNodeOpt = placementService.selectNodeForPlacement(
                        activeNodes, 2, 2048);
                
                if (selectedNodeOpt.isEmpty()) {
                    return VmCreationResult.failure(
                            ProviderError.builder()
                                    .code(ProviderError.ErrorCode.INSUFFICIENT_RESOURCES)
                                    .message("No node with sufficient resources found")
                                    .providerErrorCode("INSUFFICIENT_RESOURCES")
                                    .retryable(true)
                                    .build()
                    );
                }

                NodeEntity selectedNode = selectedNodeOpt.get();
                logger.info("Selected node {} for VM {}", selectedNode.getName(), request.vmId());

                connection = multiNodeManager.getConnection(selectedNode.getId());

                String diskPath = createDiskVolume(connection, request.vmId(), request.spec());
                if (diskPath == null) {
                    return VmCreationResult.failure(
                            ProviderError.builder()
                                    .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                                    .message("Failed to create disk image in storage pool")
                                    .providerErrorCode("LIBVIRT_STORAGE_ERROR")
                                    .retryable(false)
                                    .build()
                    );
                }

                String domainXml = LibvirtXmlBuilder.buildDomainXml(request.vmId(), request.spec(), diskPath);

                logger.debug("Libvirt domain XML for VM {}: {}", request.vmId(), domainXml);

                Domain domain = connection.domainDefineXML(domainXml);
                domain.create();

                synchronized (vmToNodeMapping) {
                    vmToNodeMapping.put(request.vmId(), selectedNode.getId());
                }

                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully created VM {} on node {} with external ID: {}",
                        request.vmId(), selectedNode.getName(), domain.getName());

                return VmCreationResult.success(domain.getName(), vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to create VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

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
            }
        });
    }

    /**
     * Creates a qcow2 disk volume in the default libvirt storage pool and returns its path.
     * The pool must exist (e.g. "default" dir pool at /var/lib/libvirt/images).
     *
     * @param connection Active libvirt connection
     * @param vmId VM UUID
     * @param spec VM spec (disk size could be parsed from here in the future)
     * @return Absolute path to the created volume, or null on failure
     */
    private String createDiskVolume(Connect connection, UUID vmId, String spec) {
        StoragePool pool = null;
        StorageVol vol = null;
        try {
            pool = connection.storagePoolLookupByName("default");
            if (pool.isActive() == 0) {
                pool.create(0);
            }
            String volName = "vm-" + vmId + ".qcow2";
            long capacityGib = LibvirtXmlBuilder.DEFAULT_DISK_CAPACITY_GIB;
            // TODO: parse spec JSON for disk size when needed
            String volXml = LibvirtXmlBuilder.buildVolumeXml(volName, capacityGib);
            vol = pool.storageVolCreateXML(volXml, 0);
            String path = vol.getPath();
            logger.debug("Created disk volume {} for VM {} at {}", volName, vmId, path);
            return path;
        } catch (LibvirtException e) {
            logger.error("Failed to create disk volume for VM {}: {}", vmId, e.getMessage(), e);
            return null;
        } finally {
            if (vol != null) {
                try {
                    vol.free();
                } catch (LibvirtException e) {
                    logger.warn("Error freeing volume: {}", e.getMessage());
                }
            }
            if (pool != null) {
                try {
                    pool.free();
                } catch (LibvirtException e) {
                    logger.warn("Error freeing pool: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Deleting VM {} from Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    logger.warn("VM {} not found on any node", request.vmId());
                    return VmDeletionResult.failure("VM not found");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmDeletionResult.failure("VM not found");
                }

                String domainName = domain.getName();

                if (domain.isActive() == 1) {
                    logger.debug("Stopping VM {} before deletion", domainName);
                    domain.destroy();
                }

                domain.undefine();

                synchronized (vmToNodeMapping) {
                    vmToNodeMapping.remove(request.vmId());
                }

                logger.info("Successfully deleted VM {} from Libvirt provider {}",
                        request.vmId(), providerId);

                return VmDeletionResult.success();

            } catch (LibvirtException e) {
                logger.error("Failed to delete VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmDeletionResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to delete VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> startVm(VmOperationRequest request) {
        logger.info("Starting VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 1) {
                    logger.debug("VM {} is already running", request.vmId());
                    return VmOperationResult.failure("VM is already running");
                }

                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully started VM {} on Libvirt provider {}",
                        request.vmId(), providerId);

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to start VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to start VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Stopping VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is already stopped", request.vmId());
                    return VmOperationResult.failure("VM is already stopped");
                }

                domain.destroy();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully stopped VM {} on Libvirt provider {}",
                        request.vmId(), providerId);

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to stop VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to stop VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Restarting VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmOperationResult.failure("VM not found");
                }

                domain.reboot(0);
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully restarted VM {} on Libvirt provider {}",
                        request.vmId(), providerId);

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to restart VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to restart VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Suspending VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmOperationResult.failure("VM not found");
                }

                if (domain.isActive() == 0) {
                    logger.debug("VM {} is not running", request.vmId());
                    return VmOperationResult.failure("VM is not running");
                }

                domain.managedSave();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully suspended VM {} on Libvirt provider {}",
                        request.vmId(), providerId);

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to suspend VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to suspend VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Resuming VM {} on Libvirt provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }

                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(
                        request.vmId().toString()
                );

                if (domain == null) {
                    logger.warn("VM {} not found on Libvirt provider {}",
                            request.vmId(), providerId);
                    return VmOperationResult.failure("VM not found");
                }

                domain.managedSaveRemove();
                domain.create();
                VmInfo vmInfo = getVmInfoFromDomain(domain);

                logger.info("Successfully resumed VM {} on Libvirt provider {}",
                        request.vmId(), providerId);

                return VmOperationResult.success(vmInfo);

            } catch (LibvirtException e) {
                logger.error("Failed to resume VM {} on Libvirt provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message("Failed to resume VM: " + e.getMessage())
                                .providerErrorCode("LIBVIRT_ERROR")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Getting VM info for {} on Libvirt provider {}", externalVmId, providerId);

        return CompletableFuture.supplyAsync(() -> {
            List<NodeEntity> nodes = multiNodeManager.getAllNodes();
            for (NodeEntity node : nodes) {
                try {
                    Connect connection = multiNodeManager.getConnection(node.getId());
                    Domain domain = connection.domainLookupByName(externalVmId);

                    if (domain != null) {
                        return Optional.of(getVmInfoFromDomain(domain));
                    }
                } catch (LibvirtException e) {
                    logger.debug("VM {} not found on node {}", externalVmId, node.getName());
                }
            }
            
            logger.debug("VM {} not found on any node", externalVmId);
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<List<VmInfo>> listVms(VmListRequest request) {
        logger.debug("Listing VMs on Libvirt provider {}", providerId);

        return CompletableFuture.supplyAsync(() -> {
            List<VmInfo> vmList = new ArrayList<>();
            List<NodeEntity> nodes = multiNodeManager.getAllNodes();

            for (NodeEntity node : nodes) {
                try {
                    Connect connection = multiNodeManager.getConnection(node.getId());
                    int[] activeDomainIds = connection.listDomains();
                    String[] inactiveDomainIds = connection.listDefinedDomains();

                    for (int domainId : activeDomainIds) {
                        try {
                            Domain domain = connection.domainLookupByID(domainId);
                            vmList.add(getVmInfoFromDomain(domain));
                        } catch (LibvirtException e) {
                            logger.warn("Failed to get info for active domain {} on node {}: {}", 
                                    domainId, node.getName(), e.getMessage());
                        }
                    }

                    for (String domainName : inactiveDomainIds) {
                        try {
                            Domain domain = connection.domainLookupByName(domainName);
                            vmList.add(getVmInfoFromDomain(domain));
                        } catch (LibvirtException e) {
                            logger.warn("Failed to get info for inactive domain {} on node {}: {}", 
                                    domainName, node.getName(), e.getMessage());
                        }
                    }
                } catch (LibvirtException e) {
                    logger.error("Failed to list VMs on node {}: {}",
                            node.getName(), e.getMessage(), e);
                }
            }

            logger.info("Found {} VMs across {} nodes", vmList.size(), nodes.size());
            return vmList;
        });
    }

    @Override
    public CompletableFuture<ProviderCapabilities> getCapabilities() {
        logger.debug("Getting capabilities for Libvirt provider {}", providerId);

        return CompletableFuture.supplyAsync(() -> {
            List<NodeEntity> nodes = multiNodeManager.getActiveNodes();
            if (nodes.isEmpty()) {
                logger.warn("No active nodes available for capability detection");
                return ProviderCapabilities.builder()
                        .supportedCpuTypes(List.of())
                        .supportedStorageClasses(List.of())
                        .supportedNetworkTypes(List.of())
                        .supportedOsTypes(List.of())
                        .resourceLimits(ResourceLimits.builder().build())
                        .features(Map.of())
                        .build();
            }

            try {
                Connect connection = multiNodeManager.getConnection(nodes.get(0).getId());
                String capabilitiesXml = connection.getCapabilities();
                LibvirtCapabilities caps = LibvirtCapabilitiesParser.parse(capabilitiesXml);

                logger.debug("Libvirt provider {} capabilities: {}", providerId, caps);

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
                                "liveMigration", caps.supportsLiveMigration(),
                                "multiNode", true
                        ))
                        .build();

            } catch (LibvirtException e) {
                logger.error("Failed to get capabilities for Libvirt provider {}: {}",
                        providerId, e.getMessage(), e);

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
    public CompletableFuture<ValidationResult> validateVmSpec(String spec) {
        logger.debug("Validating VM spec for Libvirt provider {}", providerId);

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
                logger.debug("VM spec validation passed for Libvirt provider {}", providerId);
            } else {
                logger.debug("VM spec validation failed for Libvirt provider {}: {}",
                        providerId, errors);
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
     * Finds the node where a VM is located.
     * First checks the in-memory mapping, then searches all nodes.
     *
     * @param vmId The VM UUID
     * @return The node ID where the VM is located, or null if not found
     */
    private UUID findNodeForVm(UUID vmId) {
        synchronized (vmToNodeMapping) {
            UUID cachedNodeId = vmToNodeMapping.get(vmId);
            if (cachedNodeId != null) {
                return cachedNodeId;
            }
        }

        List<NodeEntity> nodes = multiNodeManager.getAllNodes();
        for (NodeEntity node : nodes) {
            try {
                Connect connection = multiNodeManager.getConnection(node.getId());
                Domain domain = connection.domainLookupByUUIDString(vmId.toString());
                if (domain != null) {
                    synchronized (vmToNodeMapping) {
                        vmToNodeMapping.put(vmId, node.getId());
                    }
                    return node.getId();
                }
            } catch (LibvirtException e) {
                logger.debug("VM {} not found on node {}", vmId, node.getName());
            }
        }

        return null;
    }

    /**
     * Closes all open connections to Libvirt.
     */
    public void close() {
        multiNodeManager.closeAll();
    }
}
