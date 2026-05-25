package com.sal.muxon.api.dto;

import com.sal.muxon.api.enums.EntityType;

import java.util.UUID;

public class TaskTargetEntityResponse {
    private EntityType type;
    private UUID id;
    private String name;

    public EntityType getType() {
        return type;
    }

    public void setType(EntityType type) {
        this.type = type;
    }

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
}
