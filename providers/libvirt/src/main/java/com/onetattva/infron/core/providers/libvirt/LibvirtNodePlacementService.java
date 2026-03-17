package com.onetattva.infron.core.providers.libvirt;

import com.onetattva.infron.api.model.Node;
import com.onetattva.infron.api.model.Resources;
import com.onetattva.infron.db.model.NodeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class LibvirtNodePlacementService {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtNodePlacementService.class);

    public Optional<NodeEntity> selectNodeForPlacement(List<NodeEntity> nodes, int requiredCpuCores, int requiredMemMb) {
        if (nodes == null || nodes.isEmpty()) {
            logger.warn("No nodes available for placement");
            return Optional.empty();
        }

        List<NodeEntity> eligibleNodes = nodes.stream()
                .filter(node -> node.getStatus() == Node.StatusEnum.READY)
                .filter(node -> hasEnoughResources(node, requiredCpuCores, requiredMemMb))
                .toList();

        if (eligibleNodes.isEmpty()) {
            logger.warn("No eligible nodes found with required resources (CPU: {}, Memory: {} MB)", 
                    requiredCpuCores, requiredMemMb);
            return Optional.empty();
        }

        NodeEntity selectedNode = eligibleNodes.stream()
                .min(Comparator.comparingDouble(node -> calculateNodeLoad(node)))
                .orElse(null);

        if (selectedNode != null) {
            logger.info("Selected node {} for placement (CPU: {}, Memory: {} MB)", 
                    selectedNode.getName(), requiredCpuCores, requiredMemMb);
        }

        return Optional.ofNullable(selectedNode);
    }

    private boolean hasEnoughResources(NodeEntity node, int requiredCpuCores, int requiredMemMb) {
        Resources resources = node.getResources();
        
        if (resources == null) {
            Integer totalCpu = node.getCpuTotal();
            Integer totalMem = node.getMemMb();
            
            if (totalCpu == null || totalMem == null) {
                logger.debug("Node {} has no resource information, skipping", node.getName());
                return false;
            }
            
            return totalCpu >= requiredCpuCores && totalMem >= requiredMemMb;
        }

        Integer availableCpu = resources.getCpu() != null ? resources.getCpu().getAvailable() : null;
        Integer availableMem = resources.getMemory() != null ? resources.getMemory().getAvailableMb() : null;

        if (availableCpu == null || availableMem == null) {
            Integer totalCpu = resources.getCpu() != null ? resources.getCpu().getTotal() : null;
            Integer totalMem = resources.getMemory() != null ? resources.getMemory().getTotalMb() : null;
            
            if (totalCpu == null || totalMem == null) {
                logger.debug("Node {} has incomplete resource information", node.getName());
                return false;
            }
            
            return totalCpu >= requiredCpuCores && totalMem >= requiredMemMb;
        }

        boolean hasEnough = availableCpu >= requiredCpuCores && availableMem >= requiredMemMb;
        
        if (!hasEnough) {
            logger.debug("Node {} does not have enough resources. Available: CPU={}, Mem={} MB. Required: CPU={}, Mem={} MB",
                    node.getName(), availableCpu, availableMem, requiredCpuCores, requiredMemMb);
        }
        
        return hasEnough;
    }

    private double calculateNodeLoad(NodeEntity node) {
        Resources resources = node.getResources();
        
        if (resources == null) {
            return 0.0;
        }

        Integer cpuTotal = resources.getCpu() != null ? resources.getCpu().getTotal() : null;
        Integer cpuAvailable = resources.getCpu() != null ? resources.getCpu().getAvailable() : null;
        Integer memTotal = resources.getMemory() != null ? resources.getMemory().getTotalMb() : null;
        Integer memAvailable = resources.getMemory() != null ? resources.getMemory().getAvailableMb() : null;

        if (cpuTotal == null || cpuAvailable == null || memTotal == null || memAvailable == null) {
            return 0.0;
        }

        if (cpuTotal == 0 || memTotal == 0) {
            return 0.0;
        }

        double cpuUsageRatio = 1.0 - ((double) cpuAvailable / cpuTotal);
        double memUsageRatio = 1.0 - ((double) memAvailable / memTotal);

        double load = (cpuUsageRatio + memUsageRatio) / 2.0;
        
        logger.debug("Node {} load: {:.2f} (CPU usage: {:.2f}, Memory usage: {:.2f})",
                node.getName(), load, cpuUsageRatio, memUsageRatio);
        
        return load;
    }

    public Optional<NodeEntity> selectLeastLoadedNode(List<NodeEntity> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            logger.warn("No nodes available for selection");
            return Optional.empty();
        }

        List<NodeEntity> activeNodes = nodes.stream()
                .filter(node -> node.getStatus() == Node.StatusEnum.READY)
                .toList();

        if (activeNodes.isEmpty()) {
            logger.warn("No active nodes available");
            return Optional.empty();
        }

        return activeNodes.stream()
                .min(Comparator.comparingDouble(this::calculateNodeLoad));
    }
}
