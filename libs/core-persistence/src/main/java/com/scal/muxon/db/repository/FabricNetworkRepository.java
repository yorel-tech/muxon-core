package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.FabricNetworkEntity;
import com.scal.muxon.db.model.FabricNetworkEntity.FabricNetworkRole;
import com.scal.muxon.db.model.FabricNetworkEntity.FabricNetworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FabricNetworkRepository extends JpaRepository<FabricNetworkEntity, UUID> {

    List<FabricNetworkEntity> findByNodeClusterId(UUID nodeClusterId);

    List<FabricNetworkEntity> findByNodeClusterIdAndStatus(UUID nodeClusterId, FabricNetworkStatus status);

    @Query("SELECT f FROM FabricNetworkEntity f WHERE f.nodeCluster.id = :clusterId AND f.role = :role AND f.status = 'ACTIVE'")
    List<FabricNetworkEntity> findActiveByNodeClusterIdAndRole(
            @Param("clusterId") UUID clusterId,
            @Param("role") FabricNetworkRole role);

    boolean existsByNodeClusterIdAndName(UUID nodeClusterId, String name);
}
