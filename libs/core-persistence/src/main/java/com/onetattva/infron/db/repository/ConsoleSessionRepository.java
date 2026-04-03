package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ConsoleSessionEntity;
import com.onetattva.infron.db.model.ConsoleSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsoleSessionRepository extends JpaRepository<ConsoleSessionEntity, UUID> {

    Optional<ConsoleSessionEntity> findByToken(String token);

    List<ConsoleSessionEntity> findByVmIdAndUserIdAndStatus(UUID vmId, UUID userId, ConsoleSessionStatus status);

    long countByUserIdAndStatus(UUID userId, ConsoleSessionStatus status);

    List<ConsoleSessionEntity> findByStatusAndExpiresAtBefore(ConsoleSessionStatus status, Instant now);

    @Modifying
    @Query("DELETE FROM ConsoleSessionEntity c WHERE c.vmId = :vmId")
    int deleteByVmId(@Param("vmId") UUID vmId);

    @Modifying
    @Query("DELETE FROM ConsoleSessionEntity c WHERE c.expiresAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
