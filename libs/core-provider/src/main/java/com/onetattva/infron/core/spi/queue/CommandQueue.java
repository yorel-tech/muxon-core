package com.onetattva.infron.core.spi.queue;

import com.onetattva.infron.api.model.EntityType;

import java.util.List;
import java.util.UUID;

/**
 * Transport-agnostic port for sending and consuming commands.
 * Implementations may use a database table (OSS) or a message bus provided by enterprise extensions.
 */
public interface CommandQueue {

    /**
     * Enqueue a command for async processing.
     *
     * @param command the command message (id may be null; implementation may assign one)
     * @return the id assigned to the command (for DB this is the persisted entity id)
     */
    UUID sendCommand(CommandMessage command);

    /**
     * Poll for pending commands of the given entity type.
     * Implementation should apply visibility/claim semantics (e.g. set PROCESSING) so the same
     * entry is not processed by another worker. Caller must eventually call markCompleted or markFailed.
     *
     * @param entityType filter by entity type (e.g. VM)
     * @param limit      maximum number of commands to return
     * @return list of commands to process (may be empty)
     */
    List<CommandMessage> pollCommands(EntityType entityType, int limit);

    /**
     * Mark a command as successfully completed.
     *
     * @param commandId id of the command returned by pollCommands or sendCommand
     */
    void markCompleted(UUID commandId);

    /**
     * Mark a command as failed with an error message.
     *
     * @param commandId   id of the command
     * @param errorMessage error description
     */
    void markFailed(UUID commandId, String errorMessage);

    /**
     * Get stalled (old pending/processing) entries for monitoring or reset.
     * Optional: implementations may throw UnsupportedOperationException if not applicable.
     *
     * @param staleThresholdMinutes entries older than this (in minutes) are considered stalled
     * @return count of stalled entries (or 0 if not supported)
     */
    default int getStalledCount(int staleThresholdMinutes) {
        return 0;
    }

    /**
     * Reset stalled entries back to PENDING so they can be retried.
     * Optional: implementations may throw UnsupportedOperationException if not applicable.
     *
     * @param staleThresholdMinutes entries older than this are reset
     * @return number of entries reset (or 0 if not supported)
     */
    default int resetStalledEntries(int staleThresholdMinutes) {
        return 0;
    }
}
