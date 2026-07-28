package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.TenantDatacenterGrantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantDatacenterGrantRepository extends JpaRepository<TenantDatacenterGrantEntity, UUID> {

    Page<TenantDatacenterGrantEntity> findByTenant_Id(UUID tenantId, Pageable pageable);

    Optional<TenantDatacenterGrantEntity> findByTenant_IdAndDatacenter_Id(UUID tenantId, UUID datacenterId);

    Optional<TenantDatacenterGrantEntity> findByIdAndTenant_Id(UUID grantId, UUID tenantId);

    long countByTenant_Id(UUID tenantId);
}
