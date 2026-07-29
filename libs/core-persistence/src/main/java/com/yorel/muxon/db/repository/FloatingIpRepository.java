package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.FloatingIpEntity;
import com.yorel.muxon.db.model.FloatingIpEntity.FloatingIpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FloatingIpRepository extends JpaRepository<FloatingIpEntity, UUID> {

    List<FloatingIpEntity> findByTenantId(UUID tenantId);

    List<FloatingIpEntity> findByTenantIdAndDatacenterId(UUID tenantId, UUID datacenterId);

    List<FloatingIpEntity> findByTenantIdAndStatus(UUID tenantId, FloatingIpStatus status);

    Optional<FloatingIpEntity> findByAssociatedVmId(UUID vmId);
}
