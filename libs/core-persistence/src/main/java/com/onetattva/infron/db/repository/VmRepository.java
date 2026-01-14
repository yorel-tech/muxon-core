package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.VmEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.onetattva.infron.db.model.VmStatus;
import com.onetattva.infron.db.model.VmPowerState;

/**
 * Repository for VM entity operations
 */
public interface VmRepository extends JpaRepository<VmEntity, UUID> {

    /**
     * Find VM by tenant datacenter grant
     */
    @Query("SELECT v FROM VmEntity v WHERE v.tenantDatacenterGrantId = :tenantDatacenterGrantId ORDER BY v.createdAt DESC")
    Page<VmEntity> findByTenantDatacenterGrantId(UUID tenantDatacenterGrantId, Pageable pageable);

    /**
     * Find VMs by status
     */
    Page<VmEntity> findByStatus(VmStatus status, Pageable pageable);

    /**
     * Find VMs by status and tenant
     */
    @Query("SELECT v FROM VmEntity v WHERE v.status = :status AND v.tenantDatacenterGrantId = :tenantDatacenterGrantId ORDER BY v.createdAt DESC")
    Page<VmEntity> findByStatusAndTenantDatacenterGrantId(VmStatus status, UUID tenantDatacenterGrantId, Pageable pageable);

    /**
     * Find VM by ID
     */
    Optional<VmEntity> findById(UUID id);

    /**
     * Find VM by name and tenant
     */
    Optional<VmEntity> findByTenantDatacenterGrantIdAndName(UUID tenantDatacenterGrantId, String name);

    /**
     * Find VMs by tags
     */
    @Query("SELECT v FROM VmEntity v WHERE v.tenantDatacenterGrantId = :tenantDatacenterGrantId AND :tag = ANY(v.tags) ORDER BY v.createdAt DESC")
    Page<VmEntity> findByTenantDatacenterGrantIdAndTag(UUID tenantDatacenterGrantId, String tag, Pageable pageable);

    /**
     * Find VMs by provider
     */
    Page<VmEntity> findByProviderId(UUID providerId, Pageable pageable);

    /**
     * Find VM by node
     */
    Page<VmEntity> findByNodeId(UUID nodeId, Pageable pageable);

    /**
     * Find VM by external ID
     */
    Optional<VmEntity> findByProviderIdAndExternalId(UUID providerId, String externalId);

    /**
     * Count VMs by tenant datacenter grant
     */
    long countByTenantDatacenterGrantId(UUID tenantDatacenterGrantId);
}
