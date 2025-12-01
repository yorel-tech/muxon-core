package com.onetattva.infron.core.auth;

import java.util.List;

public interface AuthorizationService {
    boolean isAllowed(UserPrincipal user, String action, Scope resourceScope, Object resource);

    /**
     * Return list of tenantIds the externalId is a member of.
     */
    List<String> getTenantsForExternalId(String externalId);

    /**
     * Return true if externalId has access to tenantId.
     */
    boolean hasAccessToTenant(String externalId, String tenantId);

    /**
     * Evict cache for a subject (call this on membership changes).
     */
    void evictCacheForExternalId(String externalId);
}
