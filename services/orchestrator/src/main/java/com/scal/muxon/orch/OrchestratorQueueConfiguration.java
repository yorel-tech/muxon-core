package com.scal.muxon.orch;

/**
 * This class previously defined database-backed CommandQueue and EventPublisher
 * beans that are now provided centrally by {@link com.scal.muxon.db.queue.QueueDbConfiguration}.
 *
 * It is kept only as a placeholder to avoid breaking references; all queue
 * bean definitions live in the shared core-persistence module.
 */
public class OrchestratorQueueConfiguration {
}
