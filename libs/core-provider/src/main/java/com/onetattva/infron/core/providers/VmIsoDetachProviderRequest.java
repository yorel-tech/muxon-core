package com.onetattva.infron.core.providers;

import java.util.UUID;

public record VmIsoDetachProviderRequest(
        UUID vmId,
        String externalVmId,
        String deviceName,
        ProviderContext providerContext,
        String correlationId
) {
}
