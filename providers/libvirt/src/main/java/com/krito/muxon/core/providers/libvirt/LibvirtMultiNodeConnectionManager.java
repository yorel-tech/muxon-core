package com.krito.muxon.core.providers.libvirt;

import com.krito.muxon.api.model.Node;
import com.krito.muxon.db.model.NodeEntity;
import com.krito.muxon.db.repository.NodeRepository;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LibvirtMultiNodeConnectionManager {

    private static final Logger logger = LoggerFactory.getLogger(LibvirtMultiNodeConnectionManager.class);

    private final UUID providerId;
    private final NodeRepository nodeRepository;
    private final Map<UUID, LibvirtConnectionManager> nodeConnections = new ConcurrentHashMap<>();

    public LibvirtMultiNodeConnectionManager(UUID providerId, NodeRepository nodeRepository) {
        this.providerId = providerId;
        this.nodeRepository = nodeRepository;
    }

    public Connect getConnection(UUID nodeId) throws LibvirtException {
        LibvirtConnectionManager connectionManager = getOrCreateConnectionManager(nodeId);
        if (connectionManager == null) {
            throw new RuntimeException("Node not found or invalid: " + nodeId);
        }
        return connectionManager.getConnection();
    }

    public List<NodeEntity> getActiveNodes() {
        List<NodeEntity> allNodes = nodeRepository.findByProviderId(providerId);
        return allNodes.stream()
                .filter(node -> node.getStatus() == Node.StatusEnum.READY)
                .toList();
    }

    public List<NodeEntity> getAllNodes() {
        return nodeRepository.findByProviderId(providerId);
    }

    public Optional<NodeEntity> getNodeById(UUID nodeId) {
        return nodeRepository.findById(nodeId);
    }

    /**
     * Get any available connection from active nodes.
     * Returns the first active node connection, or null if no active nodes are available.
     *
     * @return libvirt connection or null if no active connections
     */
    public Connect getAnyConnection() {
        List<NodeEntity> activeNodes = getActiveNodes();
        if (activeNodes.isEmpty()) {
            return null;
        }
        
        // Try the first active node
        NodeEntity firstNode = activeNodes.get(0);
        try {
            return getConnection(firstNode.getId());
        } catch (LibvirtException e) {
            logger.warn("Failed to get connection for node {}: {}", firstNode.getId(), e.getMessage());
            return null;
        }
    }

    public boolean testConnection(UUID nodeId) {
        LibvirtConnectionManager connectionManager = getOrCreateConnectionManager(nodeId);
        if (connectionManager == null) {
            return false;
        }
        return connectionManager.testConnection();
    }

    public void closeAll() {
        logger.info("Closing all node connections for provider {}", providerId);
        for (Map.Entry<UUID, LibvirtConnectionManager> entry : nodeConnections.entrySet()) {
            try {
                entry.getValue().closeAll();
            } catch (Exception e) {
                logger.error("Error closing connections for node {}: {}", entry.getKey(), e.getMessage());
            }
        }
        nodeConnections.clear();
    }

    public void closeNode(UUID nodeId) {
        LibvirtConnectionManager connectionManager = nodeConnections.remove(nodeId);
        if (connectionManager != null) {
            connectionManager.closeAll();
            logger.info("Closed connections for node {}", nodeId);
        }
    }

    private LibvirtConnectionManager getOrCreateConnectionManager(UUID nodeId) {
        return nodeConnections.computeIfAbsent(nodeId, id -> {
            Optional<NodeEntity> nodeOpt = nodeRepository.findById(id);
            if (nodeOpt.isEmpty()) {
                logger.warn("Node {} not found in database", id);
                return null;
            }

            NodeEntity node = nodeOpt.get();
            if (!node.getProvider().getId().equals(providerId)) {
                logger.warn("Node {} does not belong to provider {}", id, providerId);
                return null;
            }

            String uri = buildLibvirtUriFromNode(node);
            Map<String, String> credentials = node.getCredentials() != null ? node.getCredentials() : Map.of();

            logger.debug("Creating connection manager for node {} with URI {}", id, uri);
            return new LibvirtConnectionManager(uri, credentials);
        });
    }

    private String buildLibvirtUriFromNode(NodeEntity node) {
        try {
            Map<String, String> credentials = node.getCredentials();
            if (credentials == null) {
                throw new IllegalArgumentException("Node credentials are required");
            }

            String uri = credentials.get("uri");
            if (uri != null && !uri.isBlank()) {
                logger.debug("Using explicit Libvirt URI for node {}: {}", node.getId(), uri);
                return uri.trim();
            }

            String host = credentials.get("host");
            String user = credentials.get("user");
            if (host == null || user == null) {
                throw new IllegalArgumentException(
                    "Node credentials must contain either 'uri' (e.g. qemu:///system) or both 'host' and 'user' for SSH");
            }

            String sshUri = String.format("qemu+ssh://%s@%s/system", user, host);
            logger.debug("Built SSH Libvirt URI for node {}: {}", node.getId(), sshUri);
            return sshUri;
        } catch (Exception e) {
            logger.error("Failed to build Libvirt URI from node credentials for node {}: {}",
                    node.getId(), e.getMessage(), e);
            throw new RuntimeException("Invalid node credentials for Libvirt connection", e);
        }
    }
}
