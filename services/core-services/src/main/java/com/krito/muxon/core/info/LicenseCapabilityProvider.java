package com.krito.muxon.core.info;

import com.krito.muxon.api.dto.LicenseView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Provides capabilities from the configured license.
 */
@Component
public class LicenseCapabilityProvider implements CapabilityProvider {

    private final LicenseView licenseView;

    public LicenseCapabilityProvider(
            @Value("${muxon.license.type:core}") String type,
            @Value("${muxon.license.expires:}") String expires
    ) {
        Map<String, Object> limits = new HashMap<>();
        this.licenseView = new LicenseView(type, expires.isBlank() ? null : expires, limits);
    }

    public LicenseView getLicenseView() {
        return licenseView;
    }

    @Override
    public Set<String> getCapabilities() {
        Set<String> capabilities = new HashSet<>();
        if ("enterprise".equalsIgnoreCase(licenseView.getType())) {
            capabilities.add("backup.create");
            capabilities.add("backup.schedule");
            capabilities.add("backup.restore");
            capabilities.add("monitoring.metrics");
        }
        return capabilities;
    }
}
