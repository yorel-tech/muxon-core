package com.onetattva.infron.db.queue;

import com.onetattva.infron.core.spi.queue.CommandQueue;
import com.onetattva.infron.core.spi.queue.EventPublisher;
import com.onetattva.infron.db.repository.QueueEntryRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for database-backed queue adapters.
 * Active when infron.queue.backend=db or when the property is not set (default).
 */
@Configuration
@ConditionalOnProperty(name = "infron.queue.backend", havingValue = "db", matchIfMissing = true)
public class QueueDbConfiguration {

    @Bean
    public CommandQueue commandQueue(QueueEntryRepository repository) {
        return new DbCommandQueue(repository);
    }

    @Bean
    public EventPublisher eventPublisher(QueueEntryRepository repository) {
        return new DbEventPublisher(repository);
    }
}
