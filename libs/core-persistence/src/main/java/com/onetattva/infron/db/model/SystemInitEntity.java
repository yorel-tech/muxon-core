package com.onetattva.infron.db.model;

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
     * Timestamp when this entry was last updated.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Creates a new SystemInitEntity with the specified parameters.
     */
    public SystemInitEntity(String primaryKey, String value, LocalDateTime updatedAt) {
        this.primaryKey = primaryKey;
        this.value = value;
        this.updatedAt = updatedAt;
    }

    /**
     * Creates a new SystemInitEntity with the primary key and a boolean value.
     * Used for legacy support with BOOTSTRAP_DONE_KEY.
     */
    public static SystemInitEntity createWithBoolean(String primaryKey, boolean value) {
        return new SystemInitEntity(primaryKey, value ? "true" : "false", LocalDateTime.now());
    }

    /**
     * Creates a new SystemInitEntity with the primary key, value, and system status.
     */
    public static SystemInitEntity create(String primaryKey, String value) {
        return new SystemInitEntity(primaryKey, value, LocalDateTime.now());
    }
}
