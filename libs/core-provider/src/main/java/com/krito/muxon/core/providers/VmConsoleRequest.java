package com.krito.muxon.core.providers;

import java.util.UUID;

/**
 * Request to obtain connection parameters for a VM console on the provider.
 *
 * @param vmId                    Infron VM id
 * @param tenantDatacenterGrantId Grant used for provider resolution
 * @param externalId              Provider-side VM identifier (e.g. Proxmox vmid, libvirt UUID)
 * @param nodeId                  Optional node where the VM runs (libvirt); may be null for Proxmox
 */
public record VmConsoleRequest(
        UUID vmId,
        UUID tenantDatacenterGrantId,
        String externalId,
        UUID nodeId
) {
}
