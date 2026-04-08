package com.onetattva.infron.core.orch.worker;

import com.onetattva.infron.api.model.EntityType;
import com.onetattva.infron.core.spi.queue.CommandMessage;
import com.onetattva.infron.core.spi.queue.CommandQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Polls the {@link CommandQueue} for pending tasks and dispatches them to the appropriate executor.
 *
 * <p>This class contains <em>no</em> repository dependencies — it only interacts with the three
 * queue interfaces. This boundary makes the {@code worker/} package extractable into a separate
 * service without any orchestrator/core-services coupling.
 */
@Service
public class WorkerTaskPoller {

    private static final Logger log = LoggerFactory.getLogger(WorkerTaskPoller.class);
    private static final int POLL_BATCH_SIZE = 10;
    private static final int STALL_THRESHOLD_MINUTES = 10;

    @Autowired
    private CommandQueue commandQueue;

    @Autowired
    private VmTaskExecutor vmTaskExecutor;

    @Scheduled(fixedDelay = 5000)
    public void pollVmQueue() {
        try {
            List<CommandMessage> entries = commandQueue.pollCommands(EntityType.VM, POLL_BATCH_SIZE);
            if (entries.isEmpty()) {
                return;
            }
            log.debug("Worker claimed {} VM command(s)", entries.size());
            for (CommandMessage entry : entries) {
                vmTaskExecutor.execute(entry);
            }
        } catch (Exception e) {
            log.error("Error polling VM command queue", e);
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void resetStalledEntries() {
        try {
            int reset = commandQueue.resetStalledEntries(STALL_THRESHOLD_MINUTES);
            if (reset > 0) {
                log.info("Reset {} stalled command queue entries for retry", reset);
            }
        } catch (Exception e) {
            log.error("Error resetting stalled entries", e);
        }
    }
}
