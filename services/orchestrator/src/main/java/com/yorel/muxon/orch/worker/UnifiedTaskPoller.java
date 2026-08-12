/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.orch.worker;

import com.yorel.muxon.api.model.EntityType;
import com.yorel.muxon.spi.queue.CommandMessage;
import com.yorel.muxon.spi.queue.CommandQueue;
import com.yorel.muxon.worker.TaskRouter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * OSS unified task poller - polls all entity types and routes to core-worker executors.
 *
 * <p>Disabled when {@code muxon.enterprise.enabled=true}; in that case, enterprise worker-service
 * handles polling with horizontal scaling support.
 */
@Service
@ConditionalOnProperty(
    name = "muxon.enterprise.enabled",
    havingValue = "false",
    matchIfMissing = true)
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
      for (EntityType type :
          List.of(EntityType.VM, EntityType.PROVIDER, EntityType.CONTENT_LIBRARY)) {
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
