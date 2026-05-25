package com.sal.muxon.db.repository;

import com.sal.muxon.common.Constants;
import com.sal.muxon.db.model.SystemInitEntity;
import com.sal.muxon.api.enums.BootstrapStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Repository for managing system initialization status.
 * Provides methods to query and update bootstrap status in the system_init table.
 */
public interface SystemInitRepository extends JpaRepository<SystemInitEntity, String> {

    /**
     * Find system init entry by primary key.
     *
     * @param primaryKey the primary key to search for
     * @return the SystemInitEntity if found, empty otherwise
     */
    Optional<SystemInitEntity> findByPrimaryKey(String primaryKey);

    /**
     * Update the bootstrap status for a given primary key.
     *
     * @param primaryKey the primary key to update
     * @param systemStatus the new bootstrap status
     * @param updatedAt the timestamp for the update
     * @return number of rows updated
     */
    @Modifying
    @Query(value = "UPDATE system_init SET value = :systemStatusName, system_status = cast(:systemStatusName as bootstrap_status), updated_at = :updatedAt WHERE primary_key = :primaryKey", nativeQuery = true)
    int updateSystemStatus(@Param("primaryKey") String primaryKey, @Param("systemStatus") BootstrapStatus systemStatus,
                           @Param("systemStatusName") String systemStatusName,
                           @Param("updatedAt") java.time.LocalDateTime updatedAt);

    /**
     * Check if bootstrap has been completed (status is READY).
     *
     * @return true if bootstrap status is READY, false otherwise
     */
    default boolean isBootstrapCompleted() {
        Optional<SystemInitEntity> status = findByPrimaryKey(Constants.BOOTSTRAP_STATUS_KEY);
        return status.map(SystemInitEntity::getSystemStatus).filter(BootstrapStatus.READY::equals).isPresent();
    }

    default Optional<BootstrapStatus> getBootstrapStatus() {
        Optional<SystemInitEntity> status = findByPrimaryKey(Constants.BOOTSTRAP_STATUS_KEY);
        return status.map(SystemInitEntity::getSystemStatus);
    }

    /**
     * Set the bootstrap status to BOOTSTRAPPED.
     * This is called after muxon-initializer has inserted the initial data.
     *
     * @return number of rows updated
     */
    default int markAsBootstrapped() {
        return updateSystemStatus(Constants.BOOTSTRAP_STATUS_KEY, BootstrapStatus.BOOTSTRAPPED, BootstrapStatus.BOOTSTRAPPED.name(), java.time.LocalDateTime.now());
    }

    /**
     * Set the bootstrap status to READY.
     * This is called after all required setup steps are completed.
     *
     * @return number of rows updated
     */
    default int markAsReady() {
        return updateSystemStatus(Constants.BOOTSTRAP_STATUS_KEY, BootstrapStatus.READY, BootstrapStatus.READY.name(), java.time.LocalDateTime.now());
    }
}
