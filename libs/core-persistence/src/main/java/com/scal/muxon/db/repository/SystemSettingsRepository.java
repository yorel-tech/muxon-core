package com.scal.muxon.db.repository;

import com.scal.muxon.common.Constants;
import com.scal.muxon.db.model.SystemSettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for system_settings table.
 * Supports both system-level defaults (tenant_id = SYSTEM_ID)
 * and tenant-specific overrides (tenant_id = tenant UUID).
 */
@Repository
public interface SystemSettingsRepository extends JpaRepository<SystemSettingsEntity, UUID> {

    /**
     * Find system-level settings
     */
    Optional<SystemSettingsEntity> findByTenantId(UUID tenantId);

    /**
     * Find system-level settings using SYSTEM_ID constant
     */
    default Optional<SystemSettingsEntity> findSystemSettings() {
        return findByTenantId(UUID.fromString(Constants.SYSTEM_ID));
    }

    /**
     * Check if tenant has settings override
     */
    boolean existsByTenantId(UUID tenantId);

    /**
     * Check if system settings exist
     */
    default boolean systemSettingsExist() {
        return existsByTenantId(UUID.fromString(Constants.SYSTEM_ID));
    }
}
