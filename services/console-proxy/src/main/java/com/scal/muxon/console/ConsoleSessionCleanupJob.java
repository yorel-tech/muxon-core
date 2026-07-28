package com.scal.muxon.console;

import com.scal.muxon.db.repository.ConsoleSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class ConsoleSessionCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ConsoleSessionCleanupJob.class);

    private final ConsoleSessionRepository consoleSessionRepository;

    public ConsoleSessionCleanupJob(ConsoleSessionRepository consoleSessionRepository) {
        this.consoleSessionRepository = consoleSessionRepository;
    }

    /**
     * Removes rows past {@code expires_at} so tokens cannot be revived and the table stays bounded.
     * New console opens upsert the single (vm_id, user_id) row with a fresh token.
     */
    @Scheduled(fixedDelayString = "${muxon.console.cleanup-interval-ms:60000}")
    @Transactional
    public void removeExpiredSessions() {
        Instant now = Instant.now();
        int deleted = consoleSessionRepository.deleteByExpiresAtBefore(now);
        if (deleted > 0) {
            log.info("Deleted {} expired console session row(s)", deleted);
        }
    }
}
