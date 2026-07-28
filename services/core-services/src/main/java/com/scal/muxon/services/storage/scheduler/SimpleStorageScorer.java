package com.scal.muxon.services.storage.scheduler;

import com.scal.muxon.db.model.ProviderStorageEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Simple storage scoring implementation for OSS.
 * <p>
 * Scores storage based on:
 * <ul>
 *   <li>IOPS (40%)</li>
 *   <li>Free capacity ratio (40%)</li>
 *   <li>Latency estimate (20% penalty)</li>
 * </ul>
 * </p>
 */
@Component
public class SimpleStorageScorer {

    private static final Logger log = LoggerFactory.getLogger(SimpleStorageScorer.class);

    private static final double WEIGHT_IOPS = 0.4;
    private static final double WEIGHT_CAPACITY = 0.4;
    private static final double WEIGHT_LATENCY = 0.2;

    /**
     * Score and rank provider storage candidates.
     *
     * @param candidates list of filtered candidates
     * @param context scheduler context
     * @return sorted list with highest scoring storage first
     */
    public List<ScoredStorage> scoreAndRank(
            List<ProviderStorageEntity> candidates, 
            SchedulerContext context) {
        
        List<ScoredStorage> scored = new ArrayList<>();
        
        for (ProviderStorageEntity storage : candidates) {
            double score = calculateScore(storage, context);
            scored.add(new ScoredStorage(storage, score));
        }
        
        // Sort by score descending
        scored.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        
        log.debug("Scored {} candidates, top score: {}", 
            scored.size(), scored.isEmpty() ? 0 : scored.get(0).getScore());
        
        return scored;
    }

    /**
     * Calculate score for a storage entry.
     */
    private double calculateScore(ProviderStorageEntity storage, SchedulerContext context) {
        Map<String, Object> metrics = storage.getMetrics();
        
        if (metrics == null) {
            return 0.0;
        }
        
        // IOPS score (normalized to 0-1)
        double iopsScore = normalizeIops(getMetricValue(metrics, "estimated_iops", 0));
        
        // Capacity score (free capacity ratio)
        double capacityScore = calculateCapacityScore(metrics);
        
        // Latency score (inverse, lower is better)
        double latencyScore = calculateLatencyScore(storage.getStorageType());
        
        double totalScore = (WEIGHT_IOPS * iopsScore) + 
                           (WEIGHT_CAPACITY * capacityScore) - 
                           (WEIGHT_LATENCY * latencyScore);
        
        log.debug("Storage {} score: total={}, iops={}, capacity={}, latency={}", 
            storage.getName(), totalScore, iopsScore, capacityScore, latencyScore);
        
        return totalScore;
    }

    /**
     * Normalize IOPS to 0-1 scale.
     * Assumes max IOPS of 100,000.
     */
    private double normalizeIops(double iops) {
        double maxIops = 100000.0;
        return Math.min(iops / maxIops, 1.0);
    }

    /**
     * Calculate capacity score based on free space ratio.
     */
    private double calculateCapacityScore(Map<String, Object> metrics) {
        double freeGb = getMetricValue(metrics, "free_gb", 0);
        double totalGb = getMetricValue(metrics, "total_gb", 1);
        
        if (totalGb <= 0) {
            return 0.0;
        }
        
        return freeGb / totalGb;
    }

    /**
     * Calculate latency penalty based on storage type.
     * Lower values are better (less penalty).
     */
    private double calculateLatencyScore(String storageType) {
        return switch (storageType) {
            case "nvme" -> 0.1;
            case "rbd" -> 0.2;
            case "lvm-thin", "zfspool" -> 0.3;
            case "lvm", "zfs" -> 0.4;
            case "dir", "nfs" -> 0.6;
            default -> 0.5;
        };
    }

    /**
     * Get metric value as double.
     */
    private double getMetricValue(Map<String, Object> metrics, String key, double defaultValue) {
        Object value = metrics.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Represents a scored storage entry.
     */
    public static class ScoredStorage {
        private final ProviderStorageEntity storage;
        private final double score;

        public ScoredStorage(ProviderStorageEntity storage, double score) {
            this.storage = storage;
            this.score = score;
        }

        public ProviderStorageEntity getStorage() {
            return storage;
        }

        public double getScore() {
            return score;
        }
    }
}
