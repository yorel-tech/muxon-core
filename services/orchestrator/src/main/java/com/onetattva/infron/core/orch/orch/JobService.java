package com.onetattva.infron.core.orch.orch;

import com.onetattva.infron.api.enums.EntityType;
import com.onetattva.infron.api.enums.JobStatus;
import com.onetattva.infron.api.enums.JobType;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.db.model.JobEntity;
import com.onetattva.infron.db.repository.JobRepository;
import com.onetattva.infron.grpc.workflow.v1.JobResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Core orchestrator service.
 * Creates a Job, decomposes it into tasks, enqueues tasks for worker pickup, and returns the Job
 * synchronously so callers can immediately return a job reference to the end user.
 *
 * <p>Only the {@code orch/} module writes to the {@code job} and {@code task_step} tables.
 */
@Service
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private CommandQueue commandQueue;

    /**
     * Create a job for the given workflow, enqueue its task(s) to the CommandQueue, and return
     * a gRPC-ready {@link JobResponse}.
     *
     * @param jobType       the type of workflow (e.g. VM_CREATE)
     * @param entityType    the entity this job operates on
     * @param entityId      UUID of the target entity (e.g. VM id)
     * @param correlationId caller-supplied trace/correlation id
     * @param queueType     the command queue type string (e.g. "VM_CREATE_COMMAND")
     * @param taskPayload   enriched payload forwarded to the worker via CommandQueue
     */
    @Transactional
    public JobResponse createJob(JobType jobType, EntityType entityType, UUID entityId,
                                 String correlationId, String queueType,
                                 Map<String, Object> taskPayload) {

        JobEntity job = new JobEntity();
        job.setJobType(jobType);
        job.setStatus(JobStatus.PENDING);
        job.setTargetEntityType(entityType);
        job.setTargetEntityId(entityId);
        job.setCorrelationId(correlationId);
        job.setProgressPercentage(0);
        job.setRetryCount(0);
        job.setMaxRetries(3);
        job.setCancellationRequested(false);
        job.setTotalSteps(1);
        job = jobRepository.save(job);
        log.info("Created job {} ({}) for entity {}/{}", job.getId(), jobType, entityType, entityId);

        // Enrich payload with jobId so the worker can publish task events referencing it
        Map<String, Object> enriched = new java.util.HashMap<>(taskPayload != null ? taskPayload : Map.of());
        enriched.put("jobId", job.getId().toString());

        UUID taskId = commandQueue.sendCommand(CommandMessage.builder()
                .queueType(queueType)
                .entityType(com.onetattva.infron.api.model.EntityType.valueOf(entityType.name()))
                .entityId(entityId)
                .payload(enriched)
                .correlationId(correlationId)
                .source("orchestrator")
                .actorType("SYSTEM")
                .build());

        log.debug("Enqueued task {} for job {}", taskId, job.getId());

        return toResponse(job);
    }

    /**
     * Cancel a job: mark it CANCELLED and mark any PENDING queue entries for its entity as failed.
     */
    @Transactional
    public JobResponse cancelJob(UUID jobId, String reason) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));

        if (job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.FAILED
                || job.getStatus() == JobStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel job in state: " + job.getStatus());
        }

        job.setStatus(JobStatus.CANCELLED);
        job.setCancellationRequested(true);
        job.setCancellationReason(reason);
        job.setCompletedAt(Instant.now());
        job = jobRepository.save(job);
        log.info("Cancelled job {} — {}", jobId, reason);
        return toResponse(job);
    }

    /** Read a job by id and map to gRPC response. */
    @Transactional(readOnly = true)
    public JobResponse getJob(UUID jobId) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return toResponse(job);
    }

    /** List jobs for an entity, paged. */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<JobEntity> listJobs(
            EntityType entityType, UUID entityId, JobStatus status,
            org.springframework.data.domain.Pageable pageable) {

        if (entityType != null && entityId != null && status != null) {
            return jobRepository.findByTargetEntityTypeAndTargetEntityIdAndStatus(
                    entityType, entityId, status, pageable);
        } else if (entityType != null && entityId != null) {
            return jobRepository.findByTargetEntityTypeAndTargetEntityId(
                    entityType, entityId, pageable);
        } else if (status != null) {
            return jobRepository.findByStatus(status, pageable);
        }
        return jobRepository.findAll(pageable);
    }

    public static JobResponse toResponse(JobEntity job) {
        return JobResponse.newBuilder()
                .setJobId(job.getId().toString())
                .setStatus(job.getStatus().name())
                .setEntityId(job.getTargetEntityId().toString())
                .setEntityType(job.getTargetEntityType().name())
                .setTaskCount(job.getTotalSteps() != null ? job.getTotalSteps() : 1)
                .setTasksDone(job.getProgressPercentage() != null ? job.getProgressPercentage() / 100 : 0)
                .setCreatedAt(job.getCreatedAt() != null ? job.getCreatedAt().toString() : "")
                .setErrorMessage(job.getErrorMessage() != null ? job.getErrorMessage() : "")
                .build();
    }
}
