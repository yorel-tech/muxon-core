package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.ComputeProfileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for compute profile (VM spec template) operations
 */
@Repository
public interface ComputeProfileRepository extends JpaRepository<ComputeProfileEntity, UUID> {

    /**
     * Find profiles by tenant datacenter grant
     */
    @Query("SELECT p FROM ComputeProfileEntity p WHERE p.tenantDatacenterGrantId = :tenantDatacenterGrantId ORDER BY p.createdAt DESC")
    Page<ComputeProfileEntity> findByTenantDatacenterGrantId(@Param("tenantDatacenterGrantId") UUID tenantDatacenterGrantId, Pageable pageable);

    /**
     * Find profiles by name containing search string
     */
    @Query("SELECT p FROM ComputeProfileEntity p WHERE p.name LIKE %:name% ORDER BY p.name ASC")
    Page<ComputeProfileEntity> findByNameContaining(@Param("name") String name, Pageable pageable);

    /**
     * Find profiles by tags (any match)
     */
    @Query("SELECT p FROM ComputeProfileEntity p JOIN p.tags t WHERE t IN :tags ORDER BY p.createdAt DESC")
    Page<ComputeProfileEntity> findByTags(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * Find profiles by tenant datacenter grant and name
     */
    @Query("SELECT p FROM ComputeProfileEntity p WHERE p.tenantDatacenterGrantId = :tenantDatacenterGrantId AND p.name = :name")
    ComputeProfileEntity findByTenantDatacenterGrantIdAndName(
            @Param("tenantDatacenterGrantId") UUID tenantDatacenterGrantId,
            @Param("name") String name);

    /**
     * Check if profile exists by tenant datacenter grant and name
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM ComputeProfileEntity p WHERE p.tenantDatacenterGrantId = :tenantDatacenterGrantId AND p.name = :name")
    boolean existsByTenantDatacenterGrantIdAndName(
            @Param("tenantDatacenterGrantId") UUID tenantDatacenterGrantId,
            @Param("name") String name);

    /**
     * Find system profiles (available to all tenants)
     */
    @Query("SELECT p FROM ComputeProfileEntity p WHERE p.isSystem = true ORDER BY p.createdAt DESC")
    List<ComputeProfileEntity> findSystemProfiles();
}
