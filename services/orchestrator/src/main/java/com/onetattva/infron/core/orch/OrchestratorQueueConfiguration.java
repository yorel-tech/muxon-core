package com.onetattva.infron.core.orch;

import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.EventPublisher;
import com.onetattva.infron.db.queue.DbCommandQueue;
import com.onetattva.infron.db.queue.DbEventPublisher;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides database-backed CommandQueue and EventPublisher for the orchestrator.
 * Active only when infron.queue.backend=db (default).
 */
@Configuration
@ConditionalOnProperty(name = "infron.queue.backend", havingValue = "db", matchIfMissing = true)
public class OrchestratorQueueConfiguration {

    @Bean
    public CommandQueue commandQueue(QueueEntryRepository repository) {
        return new DbCommandQueue(repository);
    }

    @Bean
    public EventPublisher eventPublisher(QueueEntryRepository repository) {
        return new DbEventPublisher(repository);
    }
}
