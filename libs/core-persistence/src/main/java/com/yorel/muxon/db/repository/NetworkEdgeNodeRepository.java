package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.NetworkEdgeNodeEntity;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeStatus;
import com.yorel.muxon.db.model.NetworkEdgeNodeEntity.NetworkEdgeNodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NetworkEdgeNodeRepository extends JpaRepository<NetworkEdgeNodeEntity, UUID> {

    List<NetworkEdgeNodeEntity> findByDatacenterId(UUID datacenterId);

    List<NetworkEdgeNodeEntity> findByDatacenterIdAndStatus(UUID datacenterId, NetworkEdgeNodeStatus status);

    Optional<NetworkEdgeNodeEntity> findFirstByDatacenterIdAndStatus(UUID datacenterId, NetworkEdgeNodeStatus status);

    boolean existsByDatacenterIdAndType(UUID datacenterId, NetworkEdgeNodeType type);
}
