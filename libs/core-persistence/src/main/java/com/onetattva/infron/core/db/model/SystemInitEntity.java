package com.onetattva.infron.db.model;

import com.onetattva.infron.api.enums.BootstrapStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Primary key for the system_init table entry.
     * Examples: "bootstrap_status", "BOOTSTRAP_DONE_KEY"
     */
    @Column(name = "primary_key", nullable = false, length = 100)
    private String primaryKey;

    /**
     * Value associated with the primary key.
     * For bootstrap_status: true/false (legacy support)
     */
    @Column(name = "value", columnDefinition = "text")
    private String value;

    /**
     * Bootstrap status indicating the current state of system initialization.
     * NOTREADY - Initial state, no bootstrap configuration found
     * BOOTSTRAPPED - Bootstrap data has been inserted (IDP, system admin, tenant)
     * READY - All required setup steps have been completed
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
     * Creates a new SystemInitEntity with the specified parameters.
     */
    public SystemInitEntity(String primaryKey, String value, BootstrapStatus systemStatus, LocalDateTime updatedAt) {
        this.primaryKey = primaryKey;
        this.value = value;
        this.systemStatus = systemStatus;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new SystemInitEntity with only the primary key and system status.
     * The value field is set to null as it's not used for bootstrap status tracking.
     */
    public static SystemInitEntity create(String primaryKey, BootstrapStatus systemStatus) {
        return new SystemInitEntity(primaryKey, null, systemStatus, LocalDateTime.now());
    }

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
    public static SystemInitEntity create(String primaryKey, String value, BootstrapStatus systemStatus) {
        return new SystemInitEntity(primaryKey, value, systemStatus, LocalDateTime.now());
    }
}
