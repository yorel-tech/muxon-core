package com.krito.muxon.db.repository;

import com.krito.muxon.db.model.ConsoleSessionEntity;
import com.krito.muxon.db.model.ConsoleSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsoleSessionRepository extends JpaRepository<ConsoleSessionEntity, UUID> {

    Optional<ConsoleSessionEntity> findByToken(String token);

    Optional<ConsoleSessionEntity> findByVmIdAndUserId(UUID vmId, UUID userId);

    /** Active sessions whose expiry is still in the future (used for per-user console cap). */
    long countByUserIdAndStatusAndExpiresAtAfter(UUID userId, ConsoleSessionStatus status, Instant expiresAt);

    @Modifying
    @Query("DELETE FROM ConsoleSessionEntity c WHERE c.vmId = :vmId")
    int deleteByVmId(@Param("vmId") UUID vmId);

    /** Removes expired rows (and any stale row past {@code now}) so the table does not grow unbounded. */
    @Modifying
    @Query("DELETE FROM ConsoleSessionEntity c WHERE c.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
}
