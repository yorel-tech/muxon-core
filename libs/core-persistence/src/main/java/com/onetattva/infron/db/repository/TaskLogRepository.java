package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TaskLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskLogRepository extends JpaRepository<TaskLogEntity, UUID> {

    List<TaskLogEntity> findByTaskIdOrderByCreatedAtDesc(UUID taskId);

    Page<TaskLogEntity> findByTaskIdOrderByCreatedAtDesc(UUID taskId, Pageable pageable);

    List<TaskLogEntity> findByStepIdOrderByCreatedAtDesc(UUID stepId);

    List<TaskLogEntity> findByTaskIdAndLogLevel(UUID taskId, String logLevel);

    @Query("SELECT l FROM TaskLogEntity l WHERE l.taskId = :taskId AND l.createdAt >= :since ORDER BY l.createdAt DESC")
    List<TaskLogEntity> findRecentLogs(@Param("taskId") UUID taskId, @Param("since") Instant since);

    @Query("SELECT COUNT(l) FROM TaskLogEntity l WHERE l.taskId = :taskId")
    long countByTaskId(@Param("taskId") UUID taskId);

    void deleteByTaskId(UUID taskId);
}
