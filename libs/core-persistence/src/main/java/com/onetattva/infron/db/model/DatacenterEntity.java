/**
 * Entity for datacenter table.
 */
package com.onetattva.infron.db.model;

import com.onetattva.infron.api.model.DatacenterCapacity;
import com.onetattva.infron.api.model.DatacenterSettings;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "datacenter")
public class DatacenterEntity {

    @Id
    // @GeneratedValue(strategy = GenerationType.AUTO) // For UUID
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Type(value = JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private DatacenterCapacity capacity;

    @Type(value = JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private DatacenterSettings settings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public DatacenterEntity() {
    }

    public DatacenterEntity(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DatacenterCapacity getCapacity() {
        return capacity;
    }

    public void setCapacity(DatacenterCapacity capacity) {
        this.capacity = capacity;
    }

    public DatacenterSettings getSettings() {
        return settings;
    }

    public void setSettings(DatacenterSettings settings) {
        this.settings = settings;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
