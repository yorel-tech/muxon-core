package com.yorel.muxon.controllers;

import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

/**
 * Base controller providing common metadata operations for entities that
 * support metadata.
 * Controllers can extend this to inherit metadata-handling methods.
 */
public abstract class BaseController {

    // Abstract methods to be implemented by subclasses
    protected abstract Map<String, String> getEntityMetadata(UUID id);

    protected abstract Map<String, String> updateEntityMetadata(UUID id, Map<String, String> metadata);

    // Common HTTP responses for metadata endpoints
    protected ResponseEntity<Map<String, String>> getMetadata(UUID id) {
        Map<String, String> metadata = getEntityMetadata(id);
        return ResponseEntity.ok(metadata);
    }

    protected ResponseEntity<Map<String, String>> updateMetadata(UUID id, Map<String, String> metadata) {
        Map<String, String> updatedMetadata = updateEntityMetadata(id, metadata);
        return ResponseEntity.ok(updatedMetadata);
    }
}
