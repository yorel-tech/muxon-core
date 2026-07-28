package com.yorel.muxon.common;

/**
 * Product edition for the Muxon backend.
 * <p>
 * This enum is used on the backend side; the value is typically serialized
 * to a lowercase string for the UI (e.g. "core", "enterprise").
 */
public enum Edition {
    CORE,
    ENTERPRISE
}

