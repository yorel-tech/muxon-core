package com.onetattva.infron.db.model;

import com.onetattva.infron.api.enums.BootstrapStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing system initialization status.
 * Tracks the bootstrap lifecycle state in the system_init table.
 */
@Entity
@Table(name = "system_init")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemInitEntity {

    /**
     * Primary key for the system_init table entry.
     * Examples: "bootstrap_status", "BOOTSTRAP_DONE_KEY"
     */
    @Id
    @Column(name = "primary_key", nullable = false, length = 100)
    private String primaryKey;

    /**
     * Value associated with the primary key.
     * For bootstrap_status: true/false (legacy support)
     */
    @Column(name = "value", columnDefinition = "text")
    private String value;

    /**
     * System status for tracking bootstrap lifecycle.
     * Uses the three-state lifecycle: NOTREADY -> BOOTSTRAPPED -> READY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "system_status", nullable = false)
    private BootstrapStatus systemStatus;

    /**
     * Timestamp when this entry was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Creates a new SystemInitEntity with the primary key and a boolean value.
     * Used for legacy support with BOOTSTRAP_DONE_KEY.
     */
    public static SystemInitEntity createWithBoolean(String primaryKey, boolean value) {
        return new SystemInitEntity(primaryKey, value ? "true" : "false", BootstrapStatus.NOTREADY, LocalDateTime.now());
    }

    /**
     * Creates a new SystemInitEntity with the primary key, value, and system status.
     */
    public static SystemInitEntity create(String primaryKey, String value) {
        return new SystemInitEntity(primaryKey, value, BootstrapStatus.NOTREADY, LocalDateTime.now());
    }

    /**
     * Creates a new SystemInitEntity with the primary key, value, and system status.
     */
    public static SystemInitEntity create(String primaryKey, String value, BootstrapStatus systemStatus) {
        return new SystemInitEntity(primaryKey, value, systemStatus, LocalDateTime.now());
    }
}
