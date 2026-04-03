package com.onetattva.infron.core.providers.proxmox;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.model.ProviderEntity;
import com.onetattva.infron.db.model.VmEntity;
import com.onetattva.infron.db.repository.NodeRepository;
import com.onetattva.infron.db.repository.ProviderRepository;
import com.onetattva.infron.db.repository.VmRepository;
import com.onetattva.infron.api.model.VmPowerState;
import com.onetattva.infron.api.model.VmStatus;
import fr.freshperf.pve4j.Proxmox;
import fr.freshperf.pve4j.SecurityConfig;
import fr.freshperf.pve4j.entities.nodes.PveNodesIndex;
import fr.freshperf.pve4j.entities.nodes.node.qemu.PveQemuCreateOptions;
import fr.freshperf.pve4j.entities.nodes.node.qemu.PveQemuIndex;
import fr.freshperf.pve4j.entities.nodes.node.qemu.PveQemuStatus;
import fr.freshperf.pve4j.entities.nodes.node.qemu.PveQemuVmVncProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Proxmox VM Provider implementation.
 * Manages VM operations on Proxmox clusters using the PVE4J library.
 * 
 * This provider is database-independent and uses external IDs directly
 * for efficient API operations without name resolution.
 */
public class ProxmoxVmProvider implements VmProvider {

    private static final Logger logger = LoggerFactory.getLogger(ProxmoxVmProvider.class);

    private final UUID providerId;
    private final ProviderRepository providerRepository;
    private final VmRepository vmRepository;
    private final NodeRepository nodeRepository;
    private final Map<String, Proxmox> clientCache = new HashMap<>();

    /**
     * Creates a new Proxmox VM provider instance.
     *
     * @param providerId The provider ID
     * @param providerRepository The provider repository for database access
     */
    public ProxmoxVmProvider(UUID providerId, ProviderRepository providerRepository, VmRepository vmRepository,
                             NodeRepository nodeRepository) {
        this.providerId = providerId;
        this.providerRepository = providerRepository;
        this.vmRepository = vmRepository;
        this.nodeRepository = nodeRepository;
    }

    @Override
    public String id() {
        return "proxmox-" + providerId;
    }

    @Override
    public String description() {
        Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
        if (providerOpt.isEmpty()) {
            return "Infron Proxmox provider (configuration not found)";
        }
        
        ProviderEntity provider = providerOpt.get();
        return "Infron Proxmox provider for cluster: " + provider.getEndpoint();
    }

    @Override
    public CompletableFuture<VmCreationResult> createVm(VmCreationRequest request) {
        logger.info("Creating VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                ProxmoxProviderContext context = (ProxmoxProviderContext) request.providerContext();
                Proxmox client = getOrCreateClient(context);
                
                // Get next available VMID from Proxmox
                int nextVmId = client.getCluster().getNextId().execute();
                String vmIdStr = String.valueOf(nextVmId);
                
                PveQemuCreateOptions createOptions = buildQemuCreateOptions(request, context);
                
                // Create VM on the target node
                String nodeName = resolveTargetNodeName(client, context);
                
                logger.info("Creating VM {} with VMID {} on node {}", request.vmId(), nextVmId, nodeName);
                
                // Create VM and wait for completion
                client.getNodes().get(nodeName).getQemu().create(nextVmId, createOptions)
                    .waitForCompletion(client)
                    .execute();
                
                boolean autoStart = true;
                
                VmInfo vmInfo = null;
                if (autoStart) {
                    logger.info("Starting VM {} on node {}", nextVmId, nodeName);
                    client.getNodes().get(nodeName).getQemu().get(nextVmId).start()
                        .waitForCompletion(client)
                        .execute();
                    vmInfo = getVmInfoFromProxmox(client, nodeName, vmIdStr);
                } else {
                    vmInfo = VmInfo.builder()
                        .externalVmId(vmIdStr)
                        .status(VmStatus.STOPPED)
                        .powerState(VmPowerState.OFF)
                        .ipAddresses(List.of())
                        .hostname(null)
                        .resourceUsage(null)
                        .metadata(Map.of(
                            "provider", "proxmox",
                            "node", nodeName,
                            "vmid", vmIdStr
                        ))
                        .lastUpdated(Instant.now())
                        .build();
                }

                logger.info("Successfully created VM {} with external ID: {}", request.vmId(), vmIdStr);

                return VmCreationResult.success(vmIdStr, vmInfo);

            } catch (Exception e) {
                logger.error("Failed to create VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmCreationResult.failure(
                    ProviderError.builder()
                            .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                            .message("Failed to create VM: " + e.getMessage())
                            .providerErrorCode("PROXMOX_ERROR")
                            .retryable(isRetryableError(e))
                            .details(Map.of(
                                "provider", "proxmox",
                                "error", e.getClass().getSimpleName(),
                                "message", e.getMessage()
                            ))
                            .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmDeletionResult> deleteVm(VmDeletionRequest request) {
        logger.info("Deleting VM {} from Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<VmEntity> vmEntityOpt = vmRepository.findById(request.vmId());
                if (vmEntityOpt.isEmpty()) {
                    return VmDeletionResult.failure("VM not found");
                }
                String vmId = vmEntityOpt.get().getExternalId();
                if (vmId == null || vmId.isBlank()) {
                    return VmDeletionResult.failure("VM has no external ID (Proxmox VMID)");
                }

                Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
                if (providerOpt.isEmpty()) {
                    return VmDeletionResult.failure("Provider not found");
                }
                
                ProxmoxProviderContext context = new ProxmoxProviderContext(
                    providerOpt.get(), "pve-node-01", "1", "local-lvm"
                );
                Proxmox client = getOrCreateClient(context);
                
                String nodeName = findVmNode(client, vmId);
                
                if (nodeName == null) {
                    logger.warn("VM {} not found on any node", vmId);
                    return VmDeletionResult.failure("VM not found");
                }

                // Stop VM if running
                try {
                    client.getNodes().get(nodeName).getQemu().get(Integer.parseInt(vmId)).getStatus()
                        .execute();
                    // VM exists, stop it first
                    client.getNodes().get(nodeName).getQemu().get(Integer.parseInt(vmId)).stop()
                        .waitForCompletion(client)
                        .execute();
                } catch (Exception e) {
                    // VM might already be stopped
                    logger.debug("VM {} might already be stopped: {}", vmId, e.getMessage());
                }

                // Delete VM
                client.getNodes().get(nodeName).getQemu().get(Integer.parseInt(vmId)).delete()
                    .waitForCompletion(client)
                    .execute();

                logger.info("Successfully deleted VM {} from Proxmox provider {}", request.vmId(), providerId);

                return VmDeletionResult.success();

            } catch (Exception e) {
                logger.error("Failed to delete VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);

                return VmDeletionResult.failure(
                    ProviderError.builder()
                            .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                            .message("Failed to delete VM: " + e.getMessage())
                            .providerErrorCode("PROXMOX_ERROR")
                            .retryable(isRetryableError(e))
                            .build()
                );
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> startVm(VmOperationRequest request) {
        logger.info("Starting VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Implementation similar to deleteVm but for start operation
                // TODO: Implement start operation
                return VmOperationResult.failure("Start operation not yet implemented");
            } catch (Exception e) {
                logger.error("Failed to start VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);
                return VmOperationResult.failure("Failed to start VM: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> stopVm(VmOperationRequest request) {
        logger.info("Stopping VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement stop operation
                return VmOperationResult.failure("Stop operation not yet implemented");
            } catch (Exception e) {
                logger.error("Failed to stop VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);
                return VmOperationResult.failure("Failed to stop VM: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> restartVm(VmOperationRequest request) {
        logger.info("Restarting VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement restart operation
                return VmOperationResult.failure("Restart operation not yet implemented");
            } catch (Exception e) {
                logger.error("Failed to restart VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);
                return VmOperationResult.failure("Failed to restart VM: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> suspendVm(VmOperationRequest request) {
        logger.info("Suspending VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement suspend operation
                return VmOperationResult.failure("Suspend operation not yet implemented");
            } catch (Exception e) {
                logger.error("Failed to suspend VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);
                return VmOperationResult.failure("Failed to suspend VM: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> resumeVm(VmOperationRequest request) {
        logger.info("Resuming VM {} on Proxmox provider {}", request.vmId(), providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                // TODO: Implement resume operation
                return VmOperationResult.failure("Resume operation not yet implemented");
            } catch (Exception e) {
                logger.error("Failed to resume VM {} on Proxmox provider {}: {}",
                        request.vmId(), providerId, e.getMessage(), e);
                return VmOperationResult.failure("Failed to resume VM: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Optional<VmInfo>> getVmInfo(String externalVmId) {
        logger.debug("Getting VM info for {} on Proxmox provider {}", externalVmId, providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
                if (providerOpt.isEmpty()) {
                    return Optional.empty();
                }
                
                ProxmoxProviderContext context = new ProxmoxProviderContext(
                    providerOpt.get(), "pve-node-01", "1", "local-lvm"
                );
                Proxmox client = getOrCreateClient(context);
                
                // Find VM across all nodes
                String nodeName = findVmNode(client, externalVmId);
                if (nodeName == null) {
                    return Optional.empty();
                }
                
                VmInfo vmInfo = getVmInfoFromProxmox(client, nodeName, externalVmId);
                return Optional.of(vmInfo);
                
            } catch (Exception e) {
                logger.error("Failed to get VM info for {} on Proxmox provider {}: {}",
                        externalVmId, providerId, e.getMessage(), e);
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<List<VmInfo>> listVms(VmListRequest request) {
        logger.debug("Listing VMs on Proxmox provider {}", providerId);

        return CompletableFuture.supplyAsync(() -> {
            List<VmInfo> vmList = new ArrayList<>();
            
            try {
                Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
                if (providerOpt.isEmpty()) {
                    return vmList;
                }
                
                ProxmoxProviderContext context = new ProxmoxProviderContext(
                    providerOpt.get(), "pve-node-01", "1", "local-lvm"
                );
                Proxmox client = getOrCreateClient(context);
                
                List<PveNodesIndex> nodes = client.getNodes().getIndex().execute();
                
                for (PveNodesIndex node : nodes) {
                    try {
                        List<PveQemuIndex> vms =
                            client.getNodes().get(node.getNode()).getQemu().getIndex().execute();
                        
                        for (PveQemuIndex vm : vms) {
                            VmInfo vmInfo = getVmInfoFromProxmox(client, node.getNode(), String.valueOf(vm.getVmid()));
                            vmList.add(vmInfo);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to list VMs on node {}: {}", node.getNode(), e.getMessage());
                    }
                }
                
                logger.info("Found {} VMs on Proxmox provider {}", vmList.size(), providerId);
                return vmList;
                
            } catch (Exception e) {
                logger.error("Failed to list VMs on Proxmox provider {}: {}", providerId, e.getMessage(), e);
                return vmList;
            }
        });
    }

    @Override
    public CompletableFuture<ProviderCapabilities> getCapabilities() {
        logger.debug("Getting capabilities for Proxmox provider {}", providerId);

        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<ProviderEntity> providerOpt = providerRepository.findById(providerId);
                if (providerOpt.isEmpty()) {
                    return getDefaultCapabilities();
                }
                
                ProxmoxProviderContext context = new ProxmoxProviderContext(
                    providerOpt.get(), "pve-node-01", "1", "local-lvm"
                );
                Proxmox client = getOrCreateClient(context);
                
                // Get cluster resources and capabilities
                // TODO: Implement proper capability discovery
                return ProviderCapabilities.builder()
                        .supportedCpuTypes(List.of("kvm64", "host"))
                        .supportedStorageClasses(List.of("local", "local-lvm", "nfs", "ceph"))
                        .supportedNetworkTypes(List.of("bridge", "ovs"))
                        .supportedOsTypes(List.of("linux", "windows", "bsd"))
                        .resourceLimits(ResourceLimits.builder()
                                .maxCpuCores(128)
                                .maxMemoryMb(1048576)  // 1TB
                                .maxStorageGb(10240)     // 10TB
                                .maxVms(1000)
                                .build())
                        .features(Map.of(
                                "cluster", true,
                                "liveMigration", true,
                                "ha", true,
                                "snapshots", true,
                                "templates", true
                        ))
                        .build();
                
            } catch (Exception e) {
                logger.error("Failed to get capabilities for Proxmox provider {}: {}",
                        providerId, e.getMessage(), e);
                return getDefaultCapabilities();
            }
        });
    }

    @Override
    public CompletableFuture<ValidationResult> validateVmSpec(String spec) {
        logger.debug("Validating VM spec for Proxmox provider {}", providerId);

        return CompletableFuture.supplyAsync(() -> {
            List<String> errors = new ArrayList<>();
            
            // Basic JSON validation
            if (spec == null || spec.trim().isEmpty()) {
                errors.add("VM spec cannot be null or empty");
            } else {
                try {
                    if (!spec.trim().startsWith("{") || !spec.trim().endsWith("}")) {
                        errors.add("VM spec must be a valid JSON object");
                    }
                } catch (Exception e) {
                    errors.add("VM spec is not valid JSON: " + e.getMessage());
                }
            }

            // Proxmox-specific validation would go here
            // TODO: Add validation for Proxmox-specific fields

            boolean valid = errors.isEmpty();
            if (valid) {
                logger.debug("VM spec validation passed for Proxmox provider {}", providerId);
            } else {
                logger.debug("VM spec validation failed for Proxmox provider {}: {}",
                        providerId, errors);
            }

            return ValidationResult.builder()
                    .valid(valid)
                    .errors(errors)
                    .build();
        });
    }

    // Helper methods
    
    private Proxmox getOrCreateClient(ProxmoxProviderContext context) {
        String cacheKey = context.getClusterEndpoint();
        return clientCache.computeIfAbsent(cacheKey, key -> {
            try {
                Map<String, String> credentials = context.getCredentials();
                String username = credentials.get("username");
                String password = credentials.get("password");
                String apiToken = credentials.get("apiToken");
                
                if (apiToken != null && !apiToken.isEmpty()) {
                    return Proxmox.create(
                        extractHost(context.getClusterEndpoint()),
                        8006,
                        apiToken,
                        SecurityConfig.insecure()
                    );
                } else {
                    String realm = credentials.get("realm");
                    if (realm == null || realm.isBlank()) {
                        realm = "pam";
                    }

                    // Accept username as "user@realm" and split for pve4j.
                    if (username != null) {
                        int atIndex = username.indexOf('@');
                        if (atIndex > 0 && atIndex < username.length() - 1) {
                            realm = username.substring(atIndex + 1);
                            username = username.substring(0, atIndex);
                        }
                    }

                    return Proxmox.createWithPassword(
                        extractHost(context.getClusterEndpoint()),
                        8006,
                        username,
                        password,
                        realm,
                        SecurityConfig.insecure()
                    );
                }
            } catch (Exception e) {
                logger.error("Failed to create Proxmox client: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to create Proxmox client", e);
            }
        });
    }
    
    private String extractHost(String endpoint) {
        // Extract host from URL like "https://pve.example.com:8006"
        if (endpoint.startsWith("http://")) {
            return endpoint.substring(7).split(":")[0];
        } else if (endpoint.startsWith("https://")) {
            return endpoint.substring(8).split(":")[0];
        }
        return endpoint.split(":")[0];
    }
    
    private PveQemuCreateOptions buildQemuCreateOptions(VmCreationRequest request, ProxmoxProviderContext context) {
        Object storage = context.getMetadata().get("storagePool");
        String storagePool = storage != null ? storage.toString() : "local-lvm";
        String diskConfig = buildDiskConfig(storagePool, request.sourceImagePath());
        var builder = PveQemuCreateOptions.builder()
                .name("vm-" + request.vmId().toString().substring(0, 8))
                .memory(2048)
                .cores(2)
                .ostype("l26")
                .onboot(true)
                .boot("order=scsi0;net0")
                .scsi(0, diskConfig)
                .net(0, "virtio,bridge=vmbr0");
        List<IsoAttachment> isos = request.isoAttachments();
        if (isos != null && !isos.isEmpty()) {
            IsoAttachment iso = isos.get(0);
            String p = iso.isoPath() != null ? iso.isoPath() : "";
            builder = builder.ide(2, p.isEmpty() ? "none,media=cdrom" : "file=" + p + ",media=cdrom");
        }
        return builder.build();
    }

    private String buildDiskConfig(String storagePool, String importFromPath) {
        String normalizedPool = storagePool != null ? storagePool.toLowerCase(Locale.ROOT) : "";
        String base;
        if (normalizedPool.contains("lvm")) {
            base = storagePool + ":8,format=raw";
        } else {
            base = storagePool + ":8,format=qcow2";
        }
        if (importFromPath != null && !importFromPath.isBlank()) {
            return base + ",import-from=" + importFromPath;
        }
        return base;
    }

    private String resolveTargetNodeName(Proxmox client, ProxmoxProviderContext context) {
        Optional<Reference> targetResource = context.getTargetResource();
        if (targetResource.isPresent()) {
            Reference ref = targetResource.get();
            // For Proxmox, API operations should use externalId (node identifier) when available.
            if (ref.externalId() != null && !ref.externalId().isBlank()) {
                return ref.externalId();
            }
            if (ref.name() != null && !ref.name().isBlank()) {
                return ref.name();
            }
        }

        try {
            List<PveNodesIndex> nodes = client.getNodes().getIndex().execute();
            if (nodes != null && !nodes.isEmpty()) {
                String discoveredNode = nodes.get(0).getNode();
                if (discoveredNode != null && !discoveredNode.isBlank()) {
                    logger.warn("No target node in provider context; using discovered Proxmox node {}", discoveredNode);
                    return discoveredNode;
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to discover fallback Proxmox node: {}", e.getMessage());
        }

        throw new IllegalStateException(
                "No target Proxmox node available. Run provider inventory sync and ensure at least one node is discovered.");
    }
    
    private String findVmNode(Proxmox client, String vmId) {
        try {
            List<PveNodesIndex> nodes = client.getNodes().getIndex().execute();
            int vmIdInt = Integer.parseInt(vmId);
            
            for (PveNodesIndex node : nodes) {
                try {
                    client.getNodes().get(node.getNode()).getQemu().get(vmIdInt).getStatus().execute();
                    return node.getNode();
                } catch (Exception e) {
                    // VM not on this node, continue
                }
            }
        } catch (Exception e) {
            logger.debug("Error searching for VM {}: {}", vmId, e.getMessage());
        }
        return null;
    }
    
    private VmInfo getVmInfoFromProxmox(Proxmox client, String nodeName, String vmId) {
        try {
            int vmIdInt = Integer.parseInt(vmId);
            PveQemuStatus status =
                client.getNodes().get(nodeName).getQemu().get(vmIdInt).getStatus().execute();
            
            VmStatus vmStatus = mapProxmoxStatus(status.getStatus());
            VmPowerState powerState = mapProxmoxPowerState(status.getStatus());
            
            List<String> ipAddresses = List.of();
            
            return VmInfo.builder()
                .externalVmId(vmId)
                .status(vmStatus)
                .powerState(powerState)
                .ipAddresses(ipAddresses)
                .hostname(status.getName())
                .resourceUsage(null)
                .metadata(Map.of(
                    "provider", "proxmox",
                    "node", nodeName,
                    "vmid", vmId,
                    "status", status.getStatus() != null ? status.getStatus() : "",
                    "uptime", String.valueOf(status.getUptime())
                ))
                .lastUpdated(Instant.now())
                .build();
                
        } catch (Exception e) {
            logger.error("Failed to get VM info for {} on node {}: {}", vmId, nodeName, e.getMessage(), e);
            return VmInfo.builder()
                .externalVmId(vmId)
                .status(VmStatus.ERROR)
                .powerState(VmPowerState.UNKNOWN)
                .ipAddresses(List.of())
                .hostname(null)
                .resourceUsage(null)
                .metadata(Map.of("error", e.getMessage() != null ? e.getMessage() : "unknown"))
                .lastUpdated(Instant.now())
                .build();
        }
    }
    
    private VmStatus mapProxmoxStatus(String proxmoxStatus) {
        if (proxmoxStatus == null || proxmoxStatus.isBlank()) {
            return VmStatus.ERROR;
        }
        return switch (proxmoxStatus.toLowerCase()) {
            case "running" -> VmStatus.ACTIVE;
            case "stopped" -> VmStatus.STOPPED;
            case "paused" -> VmStatus.SUSPENDED;
            default -> VmStatus.ERROR;
        };
    }
    
    private VmPowerState mapProxmoxPowerState(String proxmoxStatus) {
        if (proxmoxStatus == null || proxmoxStatus.isBlank()) {
            return VmPowerState.UNKNOWN;
        }
        return switch (proxmoxStatus.toLowerCase()) {
            case "running" -> VmPowerState.ON;
            case "stopped" -> VmPowerState.OFF;
            case "paused" -> VmPowerState.SUSPENDED;
            default -> VmPowerState.UNKNOWN;
        };
    }
    
    private boolean isRetryableError(Exception e) {
        if (e.getMessage() == null) {
            return false;
        }
        String message = e.getMessage().toLowerCase();
        return message.contains("timeout") || 
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("temporary");
    }
    
    private ProviderCapabilities getDefaultCapabilities() {
        return ProviderCapabilities.builder()
                .supportedCpuTypes(List.of())
                .supportedStorageClasses(List.of())
                .supportedNetworkTypes(List.of())
                .supportedOsTypes(List.of())
                .resourceLimits(ResourceLimits.builder().build())
                .features(Map.of())
                .build();
    }

    @Override
    public CompletableFuture<VmOperationResult> attachIso(VmIsoAttachProviderRequest request) {
        logger.warn("attachIso not implemented for Proxmox provider");
        return CompletableFuture.completedFuture(
                VmOperationResult.failure("ISO attach is not implemented for Proxmox"));
    }

    @Override
    public CompletableFuture<VmOperationResult> detachIso(VmIsoDetachProviderRequest request) {
        logger.warn("detachIso not implemented for Proxmox provider");
        return CompletableFuture.completedFuture(
                VmOperationResult.failure("ISO detach is not implemented for Proxmox"));
    }

    @Override
    public CompletableFuture<VmTemplateExportResult> cloneVmAsTemplate(VmTemplateExportRequest request) {
        logger.warn("cloneVmAsTemplate not implemented for Proxmox provider");
        return CompletableFuture.completedFuture(VmTemplateExportResult.failure(
                ProviderError.builder()
                        .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                        .message("Template export is not implemented for Proxmox")
                        .providerErrorCode("PROXMOX_EXPORT_NOT_IMPLEMENTED")
                        .retryable(false)
                        .build()));
    }

    @Override
    public CompletableFuture<VmConsoleConnectionInfo> getConsoleConnection(VmConsoleRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            if (request.externalId() == null || request.externalId().isBlank()) {
                throw new IllegalStateException("VM has no external id; console is unavailable until the VM is provisioned");
            }
            ProviderEntity provider = providerRepository.findById(providerId)
                    .orElseThrow(() -> new IllegalStateException("Provider configuration not found"));
            ProxmoxProviderContext ctx = new ProxmoxProviderContext(
                    provider, "node", "node", "local-lvm");
            Proxmox client = getOrCreateClient(ctx);
            String nodeName = findVmNode(client, request.externalId());
            if (nodeName == null) {
                throw new IllegalStateException("Could not locate VM on Proxmox cluster");
            }
            int vmid = Integer.parseInt(request.externalId().trim());
            try {
                PveQemuVmVncProxy proxy = client.getNodes().get(nodeName).getQemu().get(vmid).getVnc().getVncProxy().execute();
                if (proxy == null) {
                    throw new IllegalStateException("Proxmox returned empty VNC proxy response");
                }
                String host = resolveConsoleHost(provider, nodeName);
                String password = proxy.getTicket() != null && !proxy.getTicket().isBlank()
                        ? proxy.getTicket()
                        : proxy.getPassword();
                // Proxmox VNC proxy typically expects TLS on the proxy port.
                return new VmConsoleConnectionInfo(VmConsoleType.VNC, host, proxy.getPort(), password, true, null);
            } catch (Exception e) {
                logger.error("Failed to obtain Proxmox VNC proxy for vm {}: {}", request.externalId(), e.getMessage(), e);
                throw new IllegalStateException("Failed to obtain console from Proxmox: " + e.getMessage(), e);
            }
        });
    }

    private String resolveConsoleHost(ProviderEntity provider, String nodeName) {
        List<NodeEntity> nodes = nodeRepository.findByProviderId(providerId);
        for (NodeEntity n : nodes) {
            if (nodeName.equals(n.getName()) || nodeName.equals(n.getExternalId())) {
                if (n.getIpAddresses() != null && !n.getIpAddresses().isEmpty()) {
                    return n.getIpAddresses().getFirst();
                }
                break;
            }
        }
        return extractHost(provider.getEndpoint());
    }
}
