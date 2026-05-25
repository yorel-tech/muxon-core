package com.sal.muxon.spi.queue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Transport-agnostic port for task status events flowing from the worker back to the orchestrator.
 *
 * <p>OSS: backed by the {@code orchestrator_queue} table (TASK_EVENT category) in {@code DbTaskEventQueue}.
 * <p>Enterprise: replaced by a Kafka implementation in infron-nexus via {@code @ConditionalOnMissingBean}.
 *
 * <p>Event type constants: {@link TaskEventTypes}
 */
public interface TaskEventQueue {

    /**
     * Publish a task status event (called by the worker after executing a task).
     *
     * @param taskId    ID of the task (matches the commandId from CommandQueue)
     * @param eventType e.g. {@code "task.started"}, {@code "task.completed"}, {@code "task.failed"}
     * @param payload   additional details (error message, result data, etc.)
     */
    void publishTaskEvent(UUID taskId, String eventType, Map<String, Object> payload);

    /**
     * Poll for pending task events.
     * Implementation must claim entries (mark PROCESSING) so the same event is not double-processed.
     *
     * @param limit maximum number of events to return
     */
    List<TaskEventMessage> pollTaskEvents(int limit);

    /**
     * Mark a task event as successfully processed.
     */
    void markProcessed(UUID eventId);

    /**
     * Mark a task event as failed with an error description.
     */
    void markFailed(UUID eventId, String error);

    /** Well-known task event type constants. */
    interface TaskEventTypes {
        String STARTED   = "task.started";
        String COMPLETED = "task.completed";
        String FAILED    = "task.failed";
        String HEARTBEAT = "task.heartbeat";
    }
}
