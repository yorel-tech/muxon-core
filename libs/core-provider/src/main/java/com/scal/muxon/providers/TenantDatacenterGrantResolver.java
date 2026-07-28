package com.scal.muxon.providers;

import java.util.Optional;
import java.util.UUID;

/**
 * Resolves the VM provider ID for a tenant datacenter grant.
 * Implementations typically load grant and datacenter from the database and
 * return the provider ID from the linked node cluster or datacenter settings.
 */
public interface TenantDatacenterGrantResolver {

    /**
     * Resolve the provider ID for the given tenant datacenter grant.
     *
     * @param tenantDatacenterGrantId the grant ID
     * @return the provider ID to use for VM operations, or empty if not found or not linked
     */
    Optional<String> resolveProviderId(UUID tenantDatacenterGrantId);
}
