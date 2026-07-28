package com.scal.muxon.services;

import com.scal.muxon.db.model.DatacenterEntity;
import com.scal.muxon.db.model.FabricNetworkEntity;
import com.scal.muxon.db.model.FabricNetworkEntity.FabricNetworkRole;
import com.scal.muxon.db.model.FabricNetworkEntity.FabricNetworkStatus;
import com.scal.muxon.db.model.FabricNetworkEntity.FabricNetworkType;
import com.scal.muxon.db.model.NodeClusterEntity;
import com.scal.muxon.db.repository.DatacenterRepository;
import com.scal.muxon.db.repository.FabricNetworkRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FabricNetworksService {

    @Autowired
    private FabricNetworkRepository fabricNetworkRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    public List<FabricNetworkEntity> listByDatacenter(UUID datacenterId) {
        DatacenterEntity dc = getDatacenter(datacenterId);
        return fabricNetworkRepository.findByNodeClusterId(dc.getNodeCluster().getId());
    }

    @Transactional
    public FabricNetworkEntity create(UUID datacenterId, String name, FabricNetworkType type,
                                      FabricNetworkRole role, String externalId, Integer vlanId, String config) {
        if (role == null) {
            throw new IllegalArgumentException("role is required for fabric network creation");
        }
        DatacenterEntity dc = getDatacenter(datacenterId);
        NodeClusterEntity cluster = dc.getNodeCluster();

        if (fabricNetworkRepository.existsByNodeClusterIdAndName(cluster.getId(), name)) {
            throw new IllegalStateException("CONFLICT: Fabric network with name '" + name + "' already exists in this cluster");
        }

        FabricNetworkEntity entity = new FabricNetworkEntity();
        entity.setNodeCluster(cluster);
        entity.setName(name);
        entity.setType(type);
        entity.setRole(role);
        entity.setExternalId(externalId);
        entity.setVlanId(vlanId);
        entity.setConfig(config);
        entity.setStatus(FabricNetworkStatus.ACTIVE);
        return fabricNetworkRepository.save(entity);
    }

    public FabricNetworkEntity getById(UUID fabricNetworkId) {
        return fabricNetworkRepository.findById(fabricNetworkId)
                .orElseThrow(() -> new EntityNotFoundException("Fabric network not found: " + fabricNetworkId));
    }

    public void delete(UUID fabricNetworkId) {
        if (!fabricNetworkRepository.existsById(fabricNetworkId)) {
            throw new EntityNotFoundException("Fabric network not found: " + fabricNetworkId);
        }
        fabricNetworkRepository.deleteById(fabricNetworkId);
    }

    public List<FabricNetworkEntity> findActiveTenantOverlayNetworks(UUID datacenterId) {
        DatacenterEntity dc = getDatacenter(datacenterId);
        return fabricNetworkRepository.findActiveByNodeClusterIdAndRole(
                dc.getNodeCluster().getId(), FabricNetworkRole.TENANT_OVERLAY);
    }

    private DatacenterEntity getDatacenter(UUID datacenterId) {
        return datacenterRepository.findByIdWithNodeClusterAndProvider(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + datacenterId));
    }
}
