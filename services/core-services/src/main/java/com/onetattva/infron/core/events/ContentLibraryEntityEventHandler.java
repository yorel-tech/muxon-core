package com.onetattva.infron.core.events;

import com.onetattva.infron.core.spi.queue.EntityEventMessage;
import com.onetattva.infron.db.model.ContentItemDistributionEntity;
import com.onetattva.infron.db.model.ContentLibraryDistributionEntity;
import com.onetattva.infron.db.repository.ContentItemDistributionRepository;
import com.onetattva.infron.db.repository.ContentLibraryDistributionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Applies {@code content_library_distribution} / {@code content_item_distribution} updates from worker-published
 * {@link com.onetattva.infron.core.spi.queue.EntityEventQueue} events.
 */
@Component
public class ContentLibraryEntityEventHandler {

    private static final Logger log = LoggerFactory.getLogger(ContentLibraryEntityEventHandler.class);

    @Autowired
    private ContentLibraryDistributionRepository distributionRepository;

    @Autowired
    private ContentItemDistributionRepository itemDistributionRepository;

    @Transactional
    public void onItemUpdated(EntityEventMessage event) {
        UUID distributionId = event.entityId();
        Map<String, Object> p = event.payload();
        UUID contentItemId = parseUuid(p.get("contentItemId"));
        if (contentItemId == null) {
            log.warn("content_library_distribution.item_updated missing contentItemId for distribution {}", distributionId);
            return;
        }
        String status = stringVal(p.get("status"));
        if (status == null) {
            log.warn("content_library_distribution.item_updated missing status for item {}", contentItemId);
            return;
        }
        itemDistributionRepository
                .findByDistributionIdAndContentItemId(distributionId, contentItemId)
                .ifPresentOrElse(
                        row -> applyItemRow(row, p, status),
                        () -> log.warn(
                                "No content_item_distribution row for distribution {} item {}",
                                distributionId,
                                contentItemId));
        recomputeDistributionProgress(distributionId);
    }

    private static void applyItemRow(ContentItemDistributionEntity row, Map<String, Object> p, String status) {
        row.setStatus(status);
        Object sb = p.get("sizeBytes");
        if (sb instanceof Number n) {
            row.setSizeBytes(n.longValue());
        }
        Object cv = p.get("checksumVerified");
        if (cv instanceof Boolean b) {
            row.setChecksumVerified(b);
        } else if (cv != null) {
            row.setChecksumVerified(Boolean.parseBoolean(cv.toString()));
        }
        String err = stringVal(p.get("errorMessage"));
        row.setErrorMessage(err);
        row.setLastUpdatedAt(Instant.now());
    }

    private void recomputeDistributionProgress(UUID distributionId) {
        long total = itemDistributionRepository.countByDistributionId(distributionId);
        if (total <= 0) {
            return;
        }
        long terminal = itemDistributionRepository.countByDistributionIdAndStatus(distributionId, "READY")
                + itemDistributionRepository.countByDistributionIdAndStatus(distributionId, "FAILED");
        int pct = (int) ((terminal * 100L) / total);
        distributionRepository.findById(distributionId).ifPresent(d -> {
            d.setProgressPercent(Math.min(100, Math.max(0, pct)));
            distributionRepository.save(d);
        });
    }

    @Transactional
    public void onCompleted(EntityEventMessage event) {
        UUID distributionId = event.entityId();
        Map<String, Object> p = event.payload();
        distributionRepository.findById(distributionId).ifPresentOrElse(
                d -> {
                    d.setReplicateStatus("available");
                    d.setProgressPercent(100);
                    d.setErrorMessage(null);
                    d.setLastReplicatedAt(parseInstant(stringVal(p.get("lastReplicatedAt"))));
                    distributionRepository.save(d);
                    log.info("Content library distribution {} marked available", distributionId);
                },
                () -> log.warn("COMPLETED event for unknown distribution {}", distributionId));
    }

    @Transactional
    public void onFailed(EntityEventMessage event) {
        UUID distributionId = event.entityId();
        Map<String, Object> p = event.payload();
        distributionRepository.findById(distributionId).ifPresentOrElse(
                d -> {
                    d.setReplicateStatus("failed");
                    d.setErrorMessage(stringVal(p.get("errorMessage")));
                    Object pp = p.get("progressPercent");
                    if (pp instanceof Number n) {
                        d.setProgressPercent(Math.min(100, Math.max(0, n.intValue())));
                    }
                    distributionRepository.save(d);
                    log.warn("Content library distribution {} marked failed: {}", distributionId, d.getErrorMessage());
                },
                () -> log.warn("FAILED event for unknown distribution {}", distributionId));
    }

    private static String stringVal(Object o) {
        return o == null ? null : o.toString();
    }

    private static UUID parseUuid(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(iso);
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
