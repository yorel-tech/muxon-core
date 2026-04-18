package com.krito.muxon.core.services.task;

import com.krito.muxon.api.enums.JobStatus;
import com.krito.muxon.db.model.JobEntity;
import com.krito.muxon.db.repository.JobRepository;
import com.krito.muxon.db.repository.TaskLogRepository;
import com.krito.muxon.db.repository.TaskStepRepository;
import com.krito.muxon.core.services.statemachine.EntityStateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TaskMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(TaskMonitoringService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TaskStepRepository taskStepRepository;

    @Autowired
    private TaskLogRepository taskLogRepository;

    @Autowired
    private EntityStateMachine entityStateMachine;

    @Value("${muxon.tasks.stale-threshold:300}")
    private int staleThresholdSeconds;

    @Value("${muxon.tasks.cleanup-after-days:30}")
    private int cleanupAfterDays;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkTimeouts() {
        try {
            List<JobEntity> runningJobs = jobRepository.findRunningJobs();
            Instant now = Instant.now();
            int timeoutCount = 0;

            for (JobEntity job : runningJobs) {
                if (job.getTimeoutAt() != null && now.isAfter(job.getTimeoutAt())) {
                    logger.warn("Task {} has timed out (timeout: {})", job.getId(), job.getTimeoutAt());
                    
                    job.setStatus(JobStatus.FAILED);
                    job.setCompletedAt(now);
                    job.setErrorMessage("Task timed out after " + 
                        ChronoUnit.SECONDS.between(job.getStartedAt(), now) + " seconds");
                    
                    jobRepository.save(job);
                    entityStateMachine.updateStateForTaskStatus(job.getTargetEntityId(), job, JobStatus.FAILED);
                    
                    timeoutCount++;
                }
            }

            if (timeoutCount > 0) {
                logger.info("Timed out {} tasks", timeoutCount);
            }
        } catch (Exception e) {
            logger.error("Error checking task timeouts", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void checkStaleHeartbeats() {
        try {
            List<JobEntity> runningJobs = jobRepository.findRunningJobs();
            Instant staleThreshold = Instant.now().minusSeconds(staleThresholdSeconds);
            int staleCount = 0;

            for (JobEntity job : runningJobs) {
                if (job.getLastHeartbeatAt() != null && job.getLastHeartbeatAt().isBefore(staleThreshold)) {
                    logger.warn("Task {} has stale heartbeat (last: {})", job.getId(), job.getLastHeartbeatAt());
                    
                    job.setStatus(JobStatus.FAILED);
                    job.setCompletedAt(Instant.now());
                    job.setErrorMessage("Task stalled - no heartbeat for " + staleThresholdSeconds + " seconds");
                    
                    jobRepository.save(job);
                    entityStateMachine.updateStateForTaskStatus(job.getTargetEntityId(), job, JobStatus.FAILED);
                    
                    staleCount++;
                }
            }

            if (staleCount > 0) {
                logger.info("Failed {} stale tasks", staleCount);
            }
        } catch (Exception e) {
            logger.error("Error checking stale heartbeats", e);
        }
    }

    @Scheduled(fixedDelay = 3600000)
    @Transactional
    public void checkCancellationRequests() {
        try {
            List<JobEntity> runningJobs = jobRepository.findRunningJobs();
            int cancelledCount = 0;

            for (JobEntity job : runningJobs) {
                if (Boolean.TRUE.equals(job.getCancellationRequested())) {
                    logger.info("Processing cancellation request for task {}", job.getId());
                    
                    job.setStatus(JobStatus.CANCELLED);
                    job.setCompletedAt(Instant.now());
                    if (job.getErrorMessage() == null) {
                        job.setErrorMessage("Task cancelled: " + 
                            (job.getCancellationReason() != null ? job.getCancellationReason() : "User requested"));
                    }
                    
                    jobRepository.save(job);
                    entityStateMachine.updateStateForTaskStatus(job.getTargetEntityId(), job, JobStatus.CANCELLED);
                    
                    cancelledCount++;
                }
            }

            if (cancelledCount > 0) {
                logger.info("Cancelled {} tasks", cancelledCount);
            }
        } catch (Exception e) {
            logger.error("Error processing cancellation requests", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldTasks() {
        try {
            Instant cutoffDate = Instant.now().minus(cleanupAfterDays, ChronoUnit.DAYS);
            
            List<JobEntity> oldJobs = jobRepository.findAll().stream()
                .filter(job -> job.getCompletedAt() != null && job.getCompletedAt().isBefore(cutoffDate))
                .filter(job -> job.getStatus() == JobStatus.COMPLETED || 
                              job.getStatus() == JobStatus.FAILED || 
                              job.getStatus() == JobStatus.CANCELLED)
                .toList();

            int deletedCount = 0;
            for (JobEntity job : oldJobs) {
                taskLogRepository.deleteByTaskId(job.getId());
                taskStepRepository.deleteByTaskId(job.getId());
                jobRepository.delete(job);
                deletedCount++;
            }

            if (deletedCount > 0) {
                logger.info("Cleaned up {} old tasks (older than {} days)", deletedCount, cleanupAfterDays);
            }
        } catch (Exception e) {
            logger.error("Error cleaning up old tasks", e);
        }
    }

    public TaskMonitoringStats getMonitoringStats() {
        TaskMonitoringStats stats = new TaskMonitoringStats();
        stats.setPendingTasks(jobRepository.countByStatus(JobStatus.PENDING));
        stats.setRunningTasks(jobRepository.countByStatus(JobStatus.RUNNING));
        stats.setCompletedTasks(jobRepository.countByStatus(JobStatus.COMPLETED));
        stats.setFailedTasks(jobRepository.countByStatus(JobStatus.FAILED));
        stats.setCancelledTasks(jobRepository.countByStatus(JobStatus.CANCELLED));
        
        List<JobEntity> runningJobs = jobRepository.findRunningJobs();
        Instant staleThreshold = Instant.now().minusSeconds(staleThresholdSeconds);
        long staleCount = runningJobs.stream()
            .filter(job -> job.getLastHeartbeatAt() != null && job.getLastHeartbeatAt().isBefore(staleThreshold))
            .count();
        stats.setStaleTasks(staleCount);
        
        return stats;
    }

    public static class TaskMonitoringStats {
        private long pendingTasks;
        private long runningTasks;
        private long completedTasks;
        private long failedTasks;
        private long cancelledTasks;
        private long staleTasks;

        public long getPendingTasks() {
            return pendingTasks;
        }

        public void setPendingTasks(long pendingTasks) {
            this.pendingTasks = pendingTasks;
        }

        public long getRunningTasks() {
            return runningTasks;
        }

        public void setRunningTasks(long runningTasks) {
            this.runningTasks = runningTasks;
        }

        public long getCompletedTasks() {
            return completedTasks;
        }

        public void setCompletedTasks(long completedTasks) {
            this.completedTasks = completedTasks;
        }

        public long getFailedTasks() {
            return failedTasks;
        }

        public void setFailedTasks(long failedTasks) {
            this.failedTasks = failedTasks;
        }

        public long getCancelledTasks() {
            return cancelledTasks;
        }

        public void setCancelledTasks(long cancelledTasks) {
            this.cancelledTasks = cancelledTasks;
        }

        public long getStaleTasks() {
            return staleTasks;
        }

        public void setStaleTasks(long staleTasks) {
            this.staleTasks = staleTasks;
        }
    }
}
