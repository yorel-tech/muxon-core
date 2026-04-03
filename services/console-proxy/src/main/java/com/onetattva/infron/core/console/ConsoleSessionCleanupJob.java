package com.onetattva.infron.core.console;

import com.onetattva.infron.db.model.ConsoleSessionEntity;
import com.onetattva.infron.db.model.ConsoleSessionStatus;
import com.onetattva.infron.db.repository.ConsoleSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ConsoleSessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSessionCleanupJob.class);

    private final ConsoleSessionRepository consoleSessionRepository;

    public ConsoleSessionCleanupJob(ConsoleSessionRepository consoleSessionRepository) {
        this.consoleSessionRepository = consoleSessionRepository;
    }

    @Scheduled(fixedDelayString = "${infron.console.cleanup-interval-ms:60000}")
    @Transactional
    public void expireStaleSessions() {
        Instant now = Instant.now();
        List<ConsoleSessionEntity> expired =
                consoleSessionRepository.findByStatusAndExpiresAtBefore(ConsoleSessionStatus.ACTIVE, now);
        for (ConsoleSessionEntity row : expired) {
            row.setStatus(ConsoleSessionStatus.EXPIRED);
            row.setClosedAt(now);
            consoleSessionRepository.save(row);
        }
        if (!expired.isEmpty()) {
            log.info("Marked {} console session(s) as EXPIRED", expired.size());
        }
        Instant cutoff = now.minus(1, ChronoUnit.DAYS);
        int deleted = consoleSessionRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.debug("Deleted {} old console session rows", deleted);
        }
    }
}
