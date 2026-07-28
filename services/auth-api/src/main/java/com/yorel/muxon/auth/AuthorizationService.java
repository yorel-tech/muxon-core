package com.yorel.muxon.auth;

import java.util.List;

public interface AuthorizationService {
    boolean isAllowed(UserPrincipal user, String action, Scope resourceScope, Object resource);

    /**
     * Check if the user has the given permission within the specified tenant (only role bindings
     * scoped to that tenant are considered).
     */
    boolean isAllowedForTenant(UserPrincipal user, String action, String tenantId);

    /**
     * Return list of tenantIds the externalId is a member of.
     */
    List<String> getTenantsForExternalId(String externalId);

    /**
     * Return true if externalId has access to tenantId.
     */
    boolean hasAccessToTenant(String externalId, String tenantId);

    /**
     * Evict tenant cache for a subject (call this on membership changes).
     */
    void evictCacheForExternalId(String externalId);

    /**
     * Evict permission cache for a user.
     */
    void evictCacheForUser(String userId);

    /**
     * Evict all user permission caches.
     */
    void evictAllUserPermissions();
}
