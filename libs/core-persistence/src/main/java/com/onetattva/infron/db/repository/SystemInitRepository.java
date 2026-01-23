package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.SystemInitEntity;
import com.onetattva.infron.api.enums.BootstrapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing system initialization status.
 * Provides methods to query and update bootstrap status in the system_init table.
 */
public interface SystemInitRepository extends JpaRepository<SystemInitEntity, UUID> {

    /**
     * Find system init entry by primary key.
     *
     * @param primaryKey the primary key to search for
     * @return the SystemInitEntity if found, empty otherwise
     */
    Optional<SystemInitEntity> findByPrimaryKey(String primaryKey);

    /**
     * Get the current bootstrap status from system_init table.
     *
     * @return the BootstrapStatus enum value
     */
    @Query("SELECT s.systemStatus FROM SystemInitEntity s WHERE s.primaryKey = :primaryKey")
    Optional<BootstrapStatus> getSystemStatus(@Param("primaryKey") String primaryKey);

    /**
     * Update the bootstrap status for a given primary key.
     *
     * @param primaryKey the primary key to update
     * @param systemStatus the new bootstrap status
     * @return number of rows updated
     */
    @Query("UPDATE SystemInitEntity s SET s.systemStatus = :systemStatus, s.updatedAt = now() WHERE s.primaryKey = :primaryKey")
    int updateSystemStatus(@Param("primaryKey") String primaryKey, @Param("systemStatus") BootstrapStatus systemStatus);

    /**
     * Check if bootstrap has been completed (status is READY).
     *
     * @return true if bootstrap status is READY, false otherwise
     */
    default boolean isBootstrapCompleted() {
        return getSystemStatus("bootstrap_status")
                .map(status -> status == BootstrapStatus.READY)
                .orElse(false);
    }

    /**
     * Check if bootstrap has been performed (status is not NOTREADY).
     *
     * @return true if bootstrap status is BOOTSTRAPPED or READY, false otherwise
     */
    default boolean isBootstrapPerformed() {
        return getSystemStatus("bootstrap_status")
                .map(status -> status != BootstrapStatus.NOTREADY)
                .orElse(false);
    }

    /**
     * Set the bootstrap status to BOOTSTRAPPED.
     * This is called after bootstrap-initializer has inserted the initial data.
     *
     * @return number of rows updated
     */
    default int markAsBootstrapped() {
        return updateSystemStatus("bootstrap_status", BootstrapStatus.BOOTSTRAPPED);
    }

    /**
     * Set the bootstrap status to READY.
     * This is called after all required setup steps are completed.
     *
     * @return number of rows updated
     */
    default int markAsReady() {
        return updateSystemStatus("bootstrap_status", BootstrapStatus.READY);
    }
}
