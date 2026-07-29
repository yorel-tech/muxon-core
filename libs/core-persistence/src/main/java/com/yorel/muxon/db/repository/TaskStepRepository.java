package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.JobStatus;
import com.yorel.muxon.db.model.TaskStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskStepRepository extends JpaRepository<TaskStepEntity, UUID> {

    List<TaskStepEntity> findByTaskIdOrderByStepNumberAsc(UUID taskId);

    Optional<TaskStepEntity> findByTaskIdAndStepNumber(UUID taskId, Integer stepNumber);

    List<TaskStepEntity> findByTaskIdAndStatus(UUID taskId, JobStatus status);

    @Query("SELECT s FROM TaskStepEntity s WHERE s.taskId = :taskId AND s.status = 'RUNNING'")
    Optional<TaskStepEntity> findCurrentRunningStep(@Param("taskId") UUID taskId);

    @Query("SELECT COUNT(s) FROM TaskStepEntity s WHERE s.taskId = :taskId AND s.status = 'COMPLETED'")
    long countCompletedSteps(@Param("taskId") UUID taskId);

    @Query("SELECT COUNT(s) FROM TaskStepEntity s WHERE s.taskId = :taskId")
    long countTotalSteps(@Param("taskId") UUID taskId);

    void deleteByTaskId(UUID taskId);
}
