package com.scal.muxon.services;

import com.scal.muxon.db.model.DatacenterEntity;
import com.scal.muxon.db.model.NetworkEdgeNodeEntity;
import com.scal.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeStatus;
import com.scal.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeType;
import com.scal.muxon.db.repository.DatacenterRepository;
import com.scal.muxon.db.repository.NetworkEdgeNodeRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NetworkEdgeNodesService {

    @Autowired
    private NetworkEdgeNodeRepository edgeNodeRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    public List<NetworkEdgeNodeEntity> listByDatacenter(UUID datacenterId) {
        return edgeNodeRepository.findByDatacenterId(datacenterId);
    }

    @Transactional
    public NetworkEdgeNodeEntity register(UUID datacenterId, String name, String host,
                                          NetworkEdgeNodeType type, String capabilities) {
        DatacenterEntity dc = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

        // In OSS, only one STANDARD node per datacenter
        if (type == NetworkEdgeNodeType.STANDARD
                && edgeNodeRepository.existsByDatacenterIdAndType(datacenterId, NetworkEdgeNodeType.STANDARD)) {
            throw new IllegalStateException("CONFLICT: A STANDARD edge node already exists in this datacenter. Enterprise supports HA pairs.");
        }

        NetworkEdgeNodeEntity entity = new NetworkEdgeNodeEntity();
        entity.setDatacenter(dc);
        entity.setName(name);
        entity.setHost(host);
        entity.setType(type);
        entity.setStatus(NetworkEdgeNodeStatus.ACTIVE);
        entity.setCapabilities(capabilities);
        return edgeNodeRepository.save(entity);
    }

    public NetworkEdgeNodeEntity getById(UUID nodeId) {
        return edgeNodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Network edge node not found: " + nodeId));
    }

    @Transactional
    public NetworkEdgeNodeEntity update(UUID nodeId, String name, String host,
                                        NetworkEdgeNodeType type, String capabilities) {
        NetworkEdgeNodeEntity entity = getById(nodeId);
        if (name != null) entity.setName(name);
        if (host != null) entity.setHost(host);
        if (type != null) entity.setType(type);
        if (capabilities != null) entity.setCapabilities(capabilities);
        return edgeNodeRepository.save(entity);
    }

    @Transactional
    public void deactivate(UUID nodeId) {
        NetworkEdgeNodeEntity entity = getById(nodeId);
        entity.setStatus(NetworkEdgeNodeStatus.INACTIVE);
        edgeNodeRepository.save(entity);
    }

    public Optional<NetworkEdgeNodeEntity> findActiveEdgeNode(UUID datacenterId) {
        return edgeNodeRepository.findFirstByDatacenterIdAndStatus(datacenterId, NetworkEdgeNodeStatus.ACTIVE);
    }
}
