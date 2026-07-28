package com.scal.muxon.db.queue;

import com.scal.muxon.spi.queue.CommandQueue;
import com.scal.muxon.spi.queue.EntityEventQueue;
import com.scal.muxon.spi.queue.EventPublisher;
import com.scal.muxon.spi.queue.TaskEventQueue;
import com.scal.muxon.db.repository.QueueEntryRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Spring auto-configuration for database-backed queue adapters.
 *
 * <p>Because this is an {@code @AutoConfiguration}, it is loaded <em>after</em> all regular
 * {@code @Configuration} classes. Each {@code @Bean} is annotated with
 * {@code @ConditionalOnMissingBean} so that enterprise modules (e.g. infron-nexus) can replace
 * any or all queues with Kafka implementations simply by declaring their own beans — no
 * property flags or infron-core code changes required.
 *
 * <p>Enterprise override pattern in infron-nexus:
 * <pre>
 * {@literal @}Configuration
 * public class NexusQueueConfiguration {
 *     {@literal @}Bean public CommandQueue    kafkaCommandQueue(...)    { return new KafkaCommandQueue(...); }
 *     {@literal @}Bean public TaskEventQueue  kafkaTaskEventQueue(...)  { return new KafkaTaskEventQueue(...); }
 *     {@literal @}Bean public EntityEventQueue kafkaEntityEventQueue(...){ return new KafkaEntityEventQueue(...); }
 * }
 * </pre>
 */
@AutoConfiguration(after = HibernateJpaAutoConfiguration.class)
public class QueueDbConfiguration {

    @Bean
    @ConditionalOnMissingBean(CommandQueue.class)
    public CommandQueue commandQueue(QueueEntryRepository repository) {
        return new DbCommandQueue(repository);
    }

    @Bean
    @ConditionalOnMissingBean(TaskEventQueue.class)
    public TaskEventQueue taskEventQueue(QueueEntryRepository repository) {
        return new DbTaskEventQueue(repository);
    }

    @Bean
    @ConditionalOnMissingBean(EntityEventQueue.class)
    public EntityEventQueue entityEventQueue(QueueEntryRepository repository) {
        return new DbEntityEventQueue(repository);
    }

    /**
     * Legacy EventPublisher kept for backward compatibility.
     * New code should use {@link EntityEventQueue} or {@link TaskEventQueue} directly.
     */
    @Bean
    @ConditionalOnMissingBean(EventPublisher.class)
    public EventPublisher eventPublisher(QueueEntryRepository repository) {
        return new DbEventPublisher(repository);
    }
}
