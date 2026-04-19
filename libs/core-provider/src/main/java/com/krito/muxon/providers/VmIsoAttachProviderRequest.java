package com.krito.muxon.providers;

import java.util.UUID;

public record VmIsoAttachProviderRequest(
        UUID vmId,
        String externalVmId,
        String isoPath,
        String deviceName,
        boolean bootable,
        ProviderContext providerContext,
        String correlationId
) {
}
