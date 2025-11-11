/**
 * Entity for role table.
 */
package com.onetattva.infron.db.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "role")
public class RoleEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    // Constructors
    public RoleEntity() {
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
}
