package com.onetattva.infron.core.console;

import com.onetattva.infron.db.repository.ConsoleSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Deletes {@code console_session} rows past {@code expires_at}. Mirrors console-proxy cleanup so the DB stays
 * trimmed when only API services are deployed.
 */
@Component
public class ConsoleSessionTableCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSessionTableCleanupJob.class);

    private final ConsoleSessionRepository consoleSessionRepository;

    public ConsoleSessionTableCleanupJob(ConsoleSessionRepository consoleSessionRepository) {
        this.consoleSessionRepository = consoleSessionRepository;
    }

    @Scheduled(fixedDelayString = "${infron.console.cleanup-interval-ms:60000}")
    @Transactional
    public void removeExpiredSessions() {
        Instant now = Instant.now();
        int deleted = consoleSessionRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.debug("Deleted {} expired console session row(s)", deleted);
        }
    }
}
