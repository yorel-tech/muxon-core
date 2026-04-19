package com.krito.muxon.worker;

import com.krito.muxon.api.model.EntityType;
import com.krito.muxon.spi.queue.CommandMessage;
import com.krito.muxon.worker.executors.ContentTaskExecutor;
import com.krito.muxon.worker.executors.ProviderTaskExecutor;
import com.krito.muxon.worker.executors.VmTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskRouter {

    private static final Logger log = LoggerFactory.getLogger(TaskRouter.class);

    @Autowired private VmTaskExecutor vmTaskExecutor;
    @Autowired private ProviderTaskExecutor providerTaskExecutor;
    @Autowired private ContentTaskExecutor contentTaskExecutor;

    public void route(CommandMessage command) {
        if (command == null) {
            return;
        }
        EntityType type = command.entityType();
        if (type == null) {
            log.warn("Cannot route command {} (null entityType)", command.id());
            return;
        }
        switch (type) {
            case VM -> vmTaskExecutor.execute(command);
            case PROVIDER -> providerTaskExecutor.execute(command);
            case CONTENT_LIBRARY -> contentTaskExecutor.execute(command);
            default -> log.warn("No executor for entityType={} commandId={}", type, command.id());
        }
    }
}

