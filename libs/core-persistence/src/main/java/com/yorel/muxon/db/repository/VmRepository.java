package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.VmStatus;
import com.yorel.muxon.db.model.VmEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VM entity operations
 */
public interface VmRepository extends JpaRepository<VmEntity, UUID> {

    @Query("SELECT v FROM VmEntity v WHERE v.tenantDatacenterGrantId IN "
            + "(SELECT g.id FROM TenantDatacenterGrantEntity g WHERE g.tenant.id = :tenantId)")
    Page<VmEntity> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT v FROM VmEntity v WHERE v.id = :vmId AND v.tenantDatacenterGrantId IN "
            + "(SELECT g.id FROM TenantDatacenterGrantEntity g WHERE g.tenant.id = :tenantId)")
    Optional<VmEntity> findByIdAndTenantId(@Param("vmId") UUID vmId, @Param("tenantId") UUID tenantId);

    /**
     * Find VM by tenant datacenter grant
     */
    @Query("SELECT v FROM VmEntity v WHERE v.tenantDatacenterGrantId = :tenantDatacenterGrantId")
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
    @Query(value = "SELECT v FROM VmEntity v WHERE v.tenantDatacenterGrantId = :tenantDatacenterGrantId AND :tag = ANY(v.tags) ORDER BY v.createdAt DESC",
    nativeQuery = true)
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

    /**
     * Count VMs by provider
     */
    long countByProviderId(UUID providerId);

    @Query("SELECT COUNT(v) FROM VmEntity v WHERE v.tenantDatacenterGrantId IN "
            + "(SELECT g.id FROM TenantDatacenterGrantEntity g WHERE g.tenant.id = :tenantId)")
    long countByTenantId(@Param("tenantId") UUID tenantId);
}
