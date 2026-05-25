package com.sal.muxon.info;

import com.sal.muxon.api.dto.InfoResponse;
import com.sal.muxon.api.dto.LicenseView;
import com.sal.muxon.api.dto.ModuleView;
import com.sal.muxon.common.Edition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InfoService {

    private final CapabilityRegistry capabilityRegistry;
    private final ModuleRegistry moduleRegistry;
    private final Edition edition;
    private final String productName;
    private final String version;
    private final LicenseCapabilityProvider licensedCapabilityProvider;

    public InfoService(
            CapabilityRegistry capabilityRegistry,
            ModuleRegistry moduleRegistry,
            LicenseCapabilityProvider licensedCapabilityProvider,
            @Value("${muxon.edition:CORE}") String editionValue,
            @Value("${muxon.product:Infron}") String productName,
            @Value("${muxon.version:unknown}") String version
    ) {
        this.capabilityRegistry = capabilityRegistry;
        this.moduleRegistry = moduleRegistry;
        this.licensedCapabilityProvider = licensedCapabilityProvider;
        this.edition = Edition.valueOf(editionValue.toUpperCase());
        this.productName = productName;
        this.version = version;
    }

    public InfoResponse getInfo() {
        Set<String> capabilities = capabilityRegistry.resolveCapabilities();

        List<ModuleView> moduleViews = moduleRegistry.getAllModules().stream()
                .map(m -> new ModuleView(
                        m.getId(),
                        m.getDisplayName(),
                        m.getCategory(),
                        m.getMetadata()
                ))
                .collect(Collectors.toList());

        LicenseView license = licensedCapabilityProvider.getLicenseView();

        // Extras is kept flexible for simple boolean flags used directly by the UI.
        Map<String, Object> extras = Map.of();

        return new InfoResponse(
                productName,
                edition.name().toLowerCase(),
                version,
                license,
                capabilities.stream().sorted().toList(),
                moduleViews,
                extras
        );
    }
}

