package com.krito.muxon.core.providers;

import java.util.Map;
import java.util.UUID;

/**
 * Placement hints
 */
public record PlacementHints(
        UUID datacenterId,
        UUID nodeId,
        Map<String, Object> preferences
) {
    public static PlacementHintsBuilder builder() {
        return new PlacementHintsBuilder();
    }

    public static class PlacementHintsBuilder {
        private UUID datacenterId;
        private UUID nodeId;
        private Map<String, Object> preferences;

        public PlacementHintsBuilder datacenterId(UUID datacenterId) {
            this.datacenterId = datacenterId;
            return this;
        }

        public PlacementHintsBuilder nodeId(UUID nodeId) {
            this.nodeId = nodeId;
            return this;
        }

        public PlacementHintsBuilder preferences(Map<String, Object> preferences) {
            this.preferences = preferences;
            return this;
        }

        public PlacementHints build() {
            return new PlacementHints(datacenterId, nodeId, preferences);
        }
    }
}
