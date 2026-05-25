package com.sal.muxon.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Entity type enum for queue operations
 */
public enum EntityType {
    VM("VM"),
    NODE("NODE"),
    PROVIDER("PROVIDER"),
    DATACENTER("DATACENTER"),
    TENANT("TENANT"),
    CONTENT_LIBRARY("CONTENT_LIBRARY");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @JsonCreator
    public static EntityType fromValue(String value) {
        for (EntityType b : EntityType.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
