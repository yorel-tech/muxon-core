package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.core.providers.*;
import com.onetattva.infron.db.model.NodeEntity;
import com.onetattva.infron.db.repository.NodeRepository;
import com.onetattva.infron.api.model.VmPowerState;
import com.onetattva.infron.api.model.VmStatus;
import org.libvirt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                                    .code(ProviderError.ErrorCode.RESOURCE_UNAVAILABLE)
                                    .message("No node with sufficient resources found")
                                    .providerErrorCode("INSUFFICIENT_RESOURCES")
                                    .retryable(true)
                                    .build()
                    );
                }

                NodeEntity selectedNode = selectedNodeOpt.get();
                logger.info("Selected node {} for VM {}", selectedNode.getName(), request.vmId());

                connection = multiNodeManager.getConnection(selectedNode.getId());

                String diskPath;
                if (request.sourceImagePath() != null && !request.sourceImagePath().isBlank()) {
                    diskPath = createDiskFromTemplate(connection, request.vmId(), request.sourceImagePath(), request.spec());
                } else {
                    diskPath = createDiskVolume(connection, request.vmId(), request.spec());
                }
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

                List<IsoAttachment> resolvedIsos = new ArrayList<>();
                StoragePool pathPool = null;
                try {
                    pathPool = connection.storagePoolLookupByName("default");
                    if (pathPool.isActive() == 0) {
                        pathPool.create(0);
                    }
                    String poolPath = poolTargetPath(pathPool);
                    for (IsoAttachment iso : request.isoAttachments()) {
                        String abs = resolveStoragePath(poolPath, iso.isoPath());
                        resolvedIsos.add(new IsoAttachment(abs, iso.deviceName(), iso.bootable(), iso.metadata()));
                    }
                } finally {
                    if (pathPool != null) {
                        try {
                            pathPool.free();
                        } catch (LibvirtException e) {
                            logger.warn("Error freeing pool: {}", e.getMessage());
                        }
                    }
                }

                String domainXml = LibvirtXmlBuilder.buildDomainXml(
                        request.vmId(), request.spec(), diskPath, resolvedIsos);

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

    private static final int VIR_DOMAIN_AFFECT_LIVE = 1;
    private static final int VIR_DOMAIN_AFFECT_CONFIG = 2;

    private String createDiskFromTemplate(Connect connection, UUID vmId, String templatePath, String spec) {
        StoragePool pool = null;
        try {
            pool = connection.storagePoolLookupByName("default");
            if (pool.isActive() == 0) {
                pool.create(0);
            }
            String poolPath = poolTargetPath(pool);
            String backing = resolveStoragePath(poolPath, templatePath);
            String volName = "vm-" + vmId + ".qcow2";
            long capacityGib = LibvirtXmlBuilder.DEFAULT_DISK_CAPACITY_GIB;
            String volXml = LibvirtXmlBuilder.buildVolumeXml(volName, capacityGib);
            StorageVol vol = pool.storageVolCreateXML(volXml, 0);
            String destPath = vol.getPath();
            vol.free();
            vol = null;
            if (!runQemuImgCreateBacking(backing, destPath)) {
                try {
                    pool.storageVolLookupByName(volName).delete(0);
                } catch (LibvirtException ignored) {
                }
                return null;
            }
            return destPath;
        } catch (LibvirtException e) {
            logger.error("Failed to create disk from template for VM {}: {}", vmId, e.getMessage(), e);
            return null;
        } finally {
            if (pool != null) {
                try {
                    pool.free();
                } catch (LibvirtException e) {
                    logger.warn("Error freeing pool: {}", e.getMessage());
                }
            }
        }
    }

    private String poolTargetPath(StoragePool pool) throws LibvirtException {
        String xml = pool.getXMLDesc(0);
        int start = xml.indexOf("<path>");
        if (start < 0) {
            return "/var/lib/libvirt/images";
        }
        int end = xml.indexOf("</path>", start);
        if (end < 0) {
            return "/var/lib/libvirt/images";
        }
        return xml.substring(start + 6, end).trim();
    }

    private String resolveStoragePath(String poolPath, String relativeOrAbsolute) {
        if (relativeOrAbsolute == null || relativeOrAbsolute.isBlank()) {
            return "";
        }
        String p = relativeOrAbsolute.trim();
        if (p.startsWith("/")) {
            return p;
        }
        String base = poolPath.endsWith("/") ? poolPath.substring(0, poolPath.length() - 1) : poolPath;
        return base + "/" + p;
    }

    private boolean runQemuImgCreateBacking(String backingFile, String newQcow2Path) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "qemu-img", "create", "-f", "qcow2", "-F", "qcow2", "-b", backingFile, newQcow2Path);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output;
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                output = r.lines().reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            }
            int code = p.waitFor();
            if (code != 0) {
                logger.error("qemu-img create failed ({}): {}", code, output);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.error("qemu-img create error: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean runQemuImgConvert(String src, String dest) {
        try {
            Path destPath = Path.of(dest);
            if (destPath.getParent() != null) {
                Files.createDirectories(destPath.getParent());
            }
            ProcessBuilder pb = new ProcessBuilder("qemu-img", "convert", "-O", "qcow2", src, dest);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.getInputStream().readAllBytes();
            int code = p.waitFor();
            return code == 0;
        } catch (Exception e) {
            logger.error("qemu-img convert error: {}", e.getMessage(), e);
            return false;
        }
    }

    private static String extractFirstDiskSourceFile(String domainXml) {
        int diskPos = 0;
        while (diskPos < domainXml.length()) {
            int d = domainXml.indexOf("device='disk'", diskPos);
            if (d < 0) {
                d = domainXml.indexOf("device=\"disk\"", diskPos);
            }
            if (d < 0) {
                return null;
            }
            int endDisk = domainXml.indexOf("</disk>", d);
            if (endDisk < 0) {
                return null;
            }
            String chunk = domainXml.substring(d, endDisk);
            int f = chunk.indexOf("file='");
            if (f >= 0) {
                int q = chunk.indexOf('\'', f + 6);
                if (q > f) {
                    return chunk.substring(f + 6, q);
                }
            }
            f = chunk.indexOf("file=\"");
            if (f >= 0) {
                int q = chunk.indexOf('"', f + 6);
                if (q > f) {
                    return chunk.substring(f + 6, q);
                }
            }
            diskPos = endDisk + 1;
        }
        return null;
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
    public CompletableFuture<VmOperationResult> attachIso(VmIsoAttachProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }
                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(request.vmId().toString());
                StoragePool pool = connection.storagePoolLookupByName("default");
                if (pool.isActive() == 0) {
                    pool.create(0);
                }
                try {
                    String poolPath = poolTargetPath(pool);
                    String isoAbs = resolveStoragePath(poolPath, request.isoPath());
                    String dev = request.deviceName();
                    domain.attachDeviceFlags(LibvirtXmlBuilder.cdromAttachXml(isoAbs, dev),
                            VIR_DOMAIN_AFFECT_LIVE | VIR_DOMAIN_AFFECT_CONFIG);
                    return VmOperationResult.success(getVmInfoFromDomain(domain));
                } finally {
                    pool.free();
                }
            } catch (LibvirtException e) {
                logger.error("attachIso failed: {}", e.getMessage(), e);
                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message(e.getMessage())
                                .providerErrorCode("LIBVIRT_ATTACH_ISO")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build());
            }
        });
    }

    @Override
    public CompletableFuture<VmOperationResult> detachIso(VmIsoDetachProviderRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID nodeId = findNodeForVm(request.vmId());
                if (nodeId == null) {
                    return VmOperationResult.failure("VM not found on any node");
                }
                Connect connection = multiNodeManager.getConnection(nodeId);
                Domain domain = connection.domainLookupByUUIDString(request.vmId().toString());
                String xml = "<disk type='file' device='cdrom'>"
                        + "<target dev='" + request.deviceName() + "' bus='sata'/>"
                        + "</disk>";
                domain.detachDeviceFlags(xml, VIR_DOMAIN_AFFECT_LIVE | VIR_DOMAIN_AFFECT_CONFIG);
                return VmOperationResult.success(getVmInfoFromDomain(domain));
            } catch (LibvirtException e) {
                logger.error("detachIso failed: {}", e.getMessage(), e);
                return VmOperationResult.failure(
                        ProviderError.builder()
                                .code(LibvirtErrorHandler.mapLibvirtError(e))
                                .message(e.getMessage())
                                .providerErrorCode("LIBVIRT_DETACH_ISO")
                                .retryable(LibvirtErrorHandler.isRetryable(e))
                                .build());
            }
        });
    }

    @Override
    public CompletableFuture<VmTemplateExportResult> cloneVmAsTemplate(VmTemplateExportRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            StoragePool pool = null;
            try {
                Domain domain = null;
                for (NodeEntity node : multiNodeManager.getAllNodes()) {
                    try {
                        Connect connection = multiNodeManager.getConnection(node.getId());
                        try {
                            domain = connection.domainLookupByName(request.externalVmId());
                            if (domain != null) {
                                break;
                            }
                        } catch (LibvirtException e) {
                            logger.debug("Domain {} not on node {}: {}", request.externalVmId(), node.getName(), e.getMessage());
                        }
                    } catch (LibvirtException e) {
                        logger.warn("Connection failed for node {}: {}", node.getName(), e.getMessage());
                    }
                }
                if (domain == null) {
                    return VmTemplateExportResult.failure(ProviderError.builder()
                            .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                            .message("Domain not found: " + request.externalVmId())
                            .providerErrorCode("DOMAIN_NOT_FOUND")
                            .retryable(false)
                            .build());
                }
                Connect conn = domain.getConnect();
                pool = conn.storagePoolLookupByName("default");
                if (pool.isActive() == 0) {
                    pool.create(0);
                }
                String poolPath = poolTargetPath(pool);
                String src = extractFirstDiskSourceFile(domain.getXMLDesc(0));
                if (src == null || src.isBlank()) {
                    return VmTemplateExportResult.failure(ProviderError.builder()
                            .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                            .message("Could not resolve VM disk path from domain XML")
                            .providerErrorCode("NO_DISK_SOURCE")
                            .retryable(false)
                            .build());
                }
                String dest = resolveStoragePath(poolPath, request.destinationRelativePath());
                if (!runQemuImgConvert(src, dest)) {
                    return VmTemplateExportResult.failure(ProviderError.builder()
                            .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                            .message("qemu-img convert failed")
                            .providerErrorCode("QEMU_IMG_CONVERT")
                            .retryable(true)
                            .build());
                }
                long size = Files.size(Path.of(dest));
                return VmTemplateExportResult.success(dest, size, Map.of("format", "qcow2"));
            } catch (Exception e) {
                logger.error("cloneVmAsTemplate failed: {}", e.getMessage(), e);
                return VmTemplateExportResult.failure(ProviderError.builder()
                        .code(ProviderError.ErrorCode.PROVIDER_ERROR)
                        .message(e.getMessage())
                        .providerErrorCode("EXPORT_FAILED")
                        .retryable(false)
                        .build());
            } finally {
                if (pool != null) {
                    try {
                        pool.free();
                    } catch (LibvirtException e) {
                        logger.warn("Error freeing pool: {}", e.getMessage());
                    }
                }
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

    @Override
    public CompletableFuture<VmConsoleConnectionInfo> getConsoleConnection(VmConsoleRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            UUID resolvedNodeId = request.nodeId();
            if (resolvedNodeId == null) {
                resolvedNodeId = findNodeForVm(request.vmId());
            }
            if (resolvedNodeId == null) {
                throw new IllegalStateException("Could not resolve hypervisor node for VM console");
            }
            final UUID nodeId = resolvedNodeId;
            NodeEntity node = multiNodeManager.getAllNodes().stream()
                    .filter(n -> n.getId().equals(nodeId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Node not found for libvirt console"));
            String host = node.getIpAddresses() != null && !node.getIpAddresses().isEmpty()
                    ? node.getIpAddresses().getFirst()
                    : node.getName();
            Connect connection;
            try {
                connection = multiNodeManager.getConnection(nodeId);
            } catch (LibvirtException e) {
                throw new IllegalStateException("Failed to connect to libvirt: " + e.getMessage(), e);
            }
            Domain domain;
            try {
                domain = lookupDomain(connection, request.externalId(), request.vmId());
            } catch (LibvirtException e) {
                throw new IllegalStateException("Domain not found for console: " + e.getMessage(), e);
            }
            try {
                String xml = domain.getXMLDesc(0);
                return parseGraphicsFromXml(xml, host);
            } catch (LibvirtException e) {
                throw new IllegalStateException("Failed to read domain XML for console: " + e.getMessage(), e);
            }
        });
    }

    private static Domain lookupDomain(Connect connection, String externalId, UUID vmId) throws LibvirtException {
        if (externalId != null && !externalId.isBlank()) {
            try {
                return connection.domainLookupByUUIDString(externalId);
            } catch (LibvirtException ignored) {
                // try as name
            }
            try {
                return connection.domainLookupByName(externalId);
            } catch (LibvirtException e) {
                logger.debug("domainLookup by externalId failed: {}", e.getMessage());
            }
        }
        return connection.domainLookupByUUIDString(vmId.toString());
    }

    private static final Pattern GRAPHICS_TYPE_ATTR = Pattern.compile("type=['\"](vnc|spice)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAPHICS_PORT_ATTR = Pattern.compile("port=['\"](-?\\d+)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAPHICS_AUTOPORT_ATTR = Pattern.compile("autoport=['\"](yes|no)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAPHICS_LISTEN_ATTR = Pattern.compile("listen=['\"]([^'\"]*)['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAPHICS_LISTEN_CHILD_ADDRESS = Pattern.compile(
            "<listen\\s[^>]*address=['\"]([^'\"]+)['\"]", Pattern.CASE_INSENSITIVE);

    private static int indexOfIgnoreCase(String haystack, String needle) {
        return indexOfIgnoreCase(haystack, needle, 0);
    }

    private static int indexOfIgnoreCase(String haystack, String needle, int fromIndex) {
        return haystack.toLowerCase(Locale.ROOT).indexOf(needle.toLowerCase(Locale.ROOT), fromIndex);
    }

    /**
     * First {@code <graphics>...</graphics>} or self-closing {@code <graphics .../>} starting at {@code start}.
     * Quote-aware so nested {@code <listen/>} does not truncate the block early.
     */
    private static String extractGraphicsBlockFrom(String xml, int start) {
        int i = start + "<graphics".length();
        boolean inSingle = false;
        boolean inDouble = false;
        while (i < xml.length()) {
            char c = xml.charAt(i);
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
            } else if (c == '"' && !inSingle) {
                inDouble = !inDouble;
            } else if (!inSingle && !inDouble) {
                if (c == '/' && i + 1 < xml.length() && xml.charAt(i + 1) == '>') {
                    return xml.substring(start, i + 2);
                }
                if (c == '>') {
                    int close = indexOfIgnoreCase(xml, "</graphics>", i);
                    if (close < 0) {
                        return xml.substring(start, i + 1);
                    }
                    return xml.substring(start, close + "</graphics>".length());
                }
            }
            i++;
        }
        return null;
    }

    private static VmConsoleConnectionInfo parseGraphicsFromXml(String xml, String hypervisorHost) {
        int search = 0;
        while (search < xml.length()) {
            int gStart = indexOfIgnoreCase(xml, "<graphics", search);
            if (gStart < 0) {
                throw new IllegalStateException("No VNC/SPICE graphics device found in domain XML");
            }
            String block = extractGraphicsBlockFrom(xml, gStart);
            if (block == null) {
                search = gStart + 1;
                continue;
            }
            Matcher typeM = GRAPHICS_TYPE_ATTR.matcher(block);
            if (!typeM.find()) {
                search = gStart + 1;
                continue;
            }
            String gType = typeM.group(1).toLowerCase(Locale.ROOT);
            VmConsoleType consoleType = "spice".equals(gType) ? VmConsoleType.SPICE : VmConsoleType.VNC;

            Matcher portM = GRAPHICS_PORT_ATTR.matcher(block);
            int port = -1;
            if (portM.find()) {
                port = Integer.parseInt(portM.group(1));
            }
            Matcher autoM = GRAPHICS_AUTOPORT_ATTR.matcher(block);
            boolean autoport = !autoM.find() || "yes".equalsIgnoreCase(autoM.group(1));
            if (port <= 0 && autoport) {
                throw new IllegalStateException(
                        "VM uses autoport without a fixed graphics port; start the VM or assign a fixed VNC/SPICE port");
            }
            if (port <= 0) {
                port = 5900;
            }

            String listenAddr = null;
            Matcher childListen = GRAPHICS_LISTEN_CHILD_ADDRESS.matcher(block);
            if (childListen.find()) {
                listenAddr = childListen.group(1).trim();
            } else {
                Matcher listenM = GRAPHICS_LISTEN_ATTR.matcher(block);
                if (listenM.find()) {
                    listenAddr = listenM.group(1).trim();
                }
            }

            String connectHost = resolveConsoleConnectHost(hypervisorHost, listenAddr, port);
            return new VmConsoleConnectionInfo(consoleType, connectHost, port, null, false, null);
        }
        throw new IllegalStateException("No VNC/SPICE graphics device found in domain XML");
    }

    /**
     * Qemu binds VNC/SPICE to {@code listenAddr}. Remote console-proxy must use a host where that port is reachable.
     */
    private static String resolveConsoleConnectHost(String hypervisorHost, String listenAddr, int port) {
        if (listenAddr == null || listenAddr.isEmpty()) {
            return hypervisorHost;
        }
        String a = listenAddr.toLowerCase(Locale.ROOT);
        if ("0.0.0.0".equals(a) || "::".equals(a) || "[::]".equals(a)) {
            return hypervisorHost;
        }
        if ("127.0.0.1".equals(a) || "::1".equals(a) || "localhost".equals(a)) {
            throw new IllegalStateException(
                    "VM graphics listens only on loopback (" + listenAddr + ":" + port + "). "
                            + "A remote console-proxy cannot reach that address. "
                            + "Reconfigure the VM to listen on all interfaces or on the hypervisor's reachable IP "
                            + "(e.g. `virsh edit <name>`: on the <graphics> line set listen='0.0.0.0' or the node IP), "
                            + "ensure the VM is running, and open the VNC port in the hypervisor firewall.");
        }
        return listenAddr;
    }

    /**
     * Closes all open connections to Libvirt.
     */
    public void close() {
        multiNodeManager.closeAll();
    }
}
