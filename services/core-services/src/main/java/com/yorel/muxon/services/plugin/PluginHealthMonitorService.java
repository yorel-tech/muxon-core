package com.yorel.muxon.services.plugin;

import com.yorel.muxon.api.enums.PluginSource;
import com.yorel.muxon.api.enums.PluginStatus;
import com.yorel.muxon.db.model.PluginEntity;
import com.yorel.muxon.db.model.PluginHealthLogEntity;
import com.yorel.muxon.db.plugin.PluginLifecycleEvent;
import com.yorel.muxon.db.repository.PluginHealthLogRepository;
import com.yorel.muxon.db.repository.PluginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled health monitor that polls all non-BUILTIN plugins and manages
 * ACTIVE ↔ DEGRADED state transitions based on consecutive failure counts.
 */
@Service
public class PluginHealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(PluginHealthMonitorService.class);

    private static final int CONSECUTIVE_FAILURES_FOR_DEGRADED = 3;

    @Value("${muxon.plugins.health.interval-seconds:30}")
    private int intervalSeconds;

    @Autowired private PluginRepository pluginRepository;
    @Autowired private PluginHealthLogRepository healthLogRepository;
    @Autowired private PluginGrpcChannelFactory channelFactory;
    @Autowired private ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelayString = "${muxon.plugins.health.interval-seconds:30}",
               timeUnit = TimeUnit.SECONDS)
    public void pollAll() {
        List<PluginStatus> monitoredStatuses = List.of(PluginStatus.ACTIVE, PluginStatus.REGISTERED, PluginStatus.DEGRADED);
        for (PluginStatus status : monitoredStatuses) {
            pluginRepository.findByStatus(status).forEach(this::pollPlugin);
        }
    }

    /**
     * Performs an immediate synchronous health check on the given plugin.
     * Returns a result map suitable for the REST response.
     */
    @Transactional
    public Map<String, Object> checkNow(UUID pluginId) {
        PluginEntity plugin = pluginRepository.findById(pluginId)
                .orElseThrow(() -> new PluginNotFoundException("Plugin not found: " + pluginId));
        return pollPlugin(plugin);
    }

    @Transactional
    Map<String, Object> pollPlugin(PluginEntity plugin) {
        // BUILTIN plugins are implicitly healthy — skip gRPC polling.
        if (PluginSource.BUILTIN == plugin.getSource()) {
            return Map.of("healthy", true, "statusMessage", "builtin plugin", "latencyMs", 0);
        }

        long start = System.currentTimeMillis();
        boolean success = false;
        String errorDetail = null;

        try {
            success = channelFactory.checkHealth(plugin);
        } catch (Exception e) {
            errorDetail = e.getMessage();
            log.warn("Health check failed for plugin {}:{} – {}", plugin.getName(), plugin.getVersion(), e.getMessage());
        }

        int latencyMs = (int) (System.currentTimeMillis() - start);

        PluginHealthLogEntity logEntry = new PluginHealthLogEntity();
        logEntry.setPlugin(plugin);
        logEntry.setSuccess(success);
        logEntry.setErrorDetail(errorDetail);
        logEntry.setLatencyMs(latencyMs);
        healthLogRepository.save(logEntry);

        updatePluginState(plugin, success);

        return Map.of(
                "healthy", success,
                "statusMessage", errorDetail != null ? errorDetail : "ok",
                "latencyMs", latencyMs
        );
    }

    private void updatePluginState(PluginEntity plugin, boolean success) {
        if (success) {
            if (plugin.getStatus() == PluginStatus.DEGRADED) {
                plugin.setStatus(PluginStatus.ACTIVE);
                pluginRepository.save(plugin);
                eventPublisher.publishEvent(new PluginLifecycleEvent(this, plugin, "RECOVERED"));
                log.info("Plugin recovered to ACTIVE: {}:{}", plugin.getName(), plugin.getVersion());
            }
            return;
        }

        // Check consecutive failures
        List<PluginHealthLogEntity> recent = healthLogRepository.findTopNByPluginIdOrderByCheckedAtDesc(
                plugin.getId(), PageRequest.of(0, CONSECUTIVE_FAILURES_FOR_DEGRADED));

        boolean allFailed = recent.size() >= CONSECUTIVE_FAILURES_FOR_DEGRADED
                && recent.stream().noneMatch(PluginHealthLogEntity::isSuccess);

        if (allFailed && plugin.getStatus() == PluginStatus.ACTIVE) {
            plugin.setStatus(PluginStatus.DEGRADED);
            pluginRepository.save(plugin);
            eventPublisher.publishEvent(new PluginLifecycleEvent(this, plugin, "DEGRADED"));
            log.warn("Plugin entered DEGRADED state after {} consecutive failures: {}:{}",
                    CONSECUTIVE_FAILURES_FOR_DEGRADED, plugin.getName(), plugin.getVersion());
        }
    }
}
