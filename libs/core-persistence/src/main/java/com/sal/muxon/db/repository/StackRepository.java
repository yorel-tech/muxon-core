package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.StackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StackRepository extends JpaRepository<StackEntity, UUID> {

    List<StackEntity> findByTenantDatacenterGrantId(UUID tenantDatacenterGrantId);

    Optional<StackEntity> findByTenantDatacenterGrantIdAndName(UUID tenantDatacenterGrantId, String name);

    boolean existsByTenantDatacenterGrantIdAndName(UUID tenantDatacenterGrantId, String name);

    @Query("SELECT COUNT(v) > 0 FROM VmEntity v WHERE v.stackId = :stackId AND v.status != 'DELETED'")
    boolean hasActiveVms(@Param("stackId") UUID stackId);
}
