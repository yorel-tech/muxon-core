package com.sal.muxon.customization.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Serialisable customization status stored in {@code vms.customization_status} JSONB.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomizationStatus {

    private CustomizationPhase phase = CustomizationPhase.NONE;
    /** Fine-grained Linux sub-phase: NETWORK_CONFIG, CONFIG, SCRIPTS_PRE, SCRIPTS_POST. */
    private String subPhase;
    private String message;
    private Instant startedAt;
    private Instant completedAt;

    public CustomizationStatus() {}

    public static CustomizationStatus none() {
        return new CustomizationStatus();
    }

    public static CustomizationStatus pending() {
        CustomizationStatus s = new CustomizationStatus();
        s.phase = CustomizationPhase.PENDING;
        s.startedAt = Instant.now();
        return s;
    }

    public CustomizationPhase getPhase() { return phase; }
    public void setPhase(CustomizationPhase phase) { this.phase = phase; }

    public String getSubPhase() { return subPhase; }
    public void setSubPhase(String subPhase) { this.subPhase = subPhase; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
