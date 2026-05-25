package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.VpcEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VpcRepository extends JpaRepository<VpcEntity, UUID> {

    List<VpcEntity> findByTenantId(UUID tenantId);

    Optional<VpcEntity> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    long countByTenantId(UUID tenantId);
}
