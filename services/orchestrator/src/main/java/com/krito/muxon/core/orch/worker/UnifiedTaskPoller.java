package com.krito.muxon.core.orch.worker;

import com.krito.muxon.api.model.EntityType;
import com.krito.muxon.core.spi.queue.CommandMessage;
import com.krito.muxon.core.spi.queue.CommandQueue;
import com.krito.muxon.core.worker.TaskRouter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OSS unified task poller - polls all entity types and routes to core-worker executors.
 *
 * <p>Disabled when {@code muxon.enterprise.enabled=true}; in that case, enterprise worker-service
 * handles polling with horizontal scaling support.
 */
@Service
@ConditionalOnProperty(name = "muxon.enterprise.enabled", havingValue = "false", matchIfMissing = true)
public class UnifiedTaskPoller {

    private static final Logger log = LoggerFactory.getLogger(UnifiedTaskPoller.class);

    @Value("${muxon.worker.poll-batch-size:10}")
    private int pollBatchSize;

    @Value("${muxon.worker.stall-threshold-minutes:10}")
    private int stallThresholdMinutes;

    @Autowired private CommandQueue commandQueue;
    @Autowired private TaskRouter taskRouter;

    @Scheduled(fixedDelayString = "${muxon.worker.poll-delay-ms:5000}")
    public void pollAllQueues() {
        try {
            for (EntityType type : List.of(EntityType.VM, EntityType.PROVIDER, EntityType.CONTENT_LIBRARY)) {
                List<CommandMessage> commands = commandQueue.pollCommands(type, pollBatchSize);
                if (commands.isEmpty()) {
                    continue;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Worker claimed {} {} command(s)", commands.size(), type);
                }
                for (CommandMessage cmd : commands) {
                    try {
                        taskRouter.route(cmd);
                    } catch (Exception e) {
                        log.error("Task execution failed for command {}: {}", cmd.id(), e.getMessage(), e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error polling command queues", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void resetStalledEntries() {
        try {
            int reset = commandQueue.resetStalledEntries(stallThresholdMinutes);
            if (reset > 0) {
                log.info("Reset {} stalled command queue entries for retry", reset);
            }
        } catch (Exception e) {
            log.error("Error resetting stalled entries", e);
        }
    }
}

