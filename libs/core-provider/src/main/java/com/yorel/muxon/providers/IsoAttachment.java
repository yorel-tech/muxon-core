package com.yorel.muxon.providers;

import java.util.Map;

/**
 * Resolved ISO path and hints from content library metadata for VM provisioning.
 */
public record IsoAttachment(
        String isoPath,
        String deviceName,
        boolean bootable,
        Map<String, String> metadata
) {
}
