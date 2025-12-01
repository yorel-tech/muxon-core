package com.onetattva.infron.core.services;

import com.onetattva.infron.core.auth.AuthorizationService;
import com.onetattva.infron.core.auth.Scope;
import com.onetattva.infron.core.auth.UserPrincipal;
import com.onetattva.infron.db.model.UserRoleBindingViewEntity;
import com.onetattva.infron.db.repository.PermissionRepository;
import com.onetattva.infron.db.repository.UserRoleBindingViewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class AuthorizationServiceImpl implements AuthorizationService {

    private final PermissionRepository permissionRepository; // your existing repo/logic
    private final RestTemplate restTemplate;
    private final boolean opaEnabled;
    private final String opaUrl;

    private final UserRoleBindingViewRepository userRoleRepo;
    private final StringRedisTemplate redis;
    private final Duration cacheTtl;


    public AuthorizationServiceImpl(PermissionRepository permissionRepository,
                            RestTemplate restTemplate,
                            @Value("${authz.opa.enabled:false}") boolean opaEnabled,
                            @Value("${authz.opa.url:}") String opaUrl,
                            UserRoleBindingViewRepository tenantUserRepository,
                            StringRedisTemplate redis,
                            @Value("${authz.cache.ttl-seconds:300}") long ttlSeconds) {
        this.permissionRepository = permissionRepository;
        this.restTemplate = restTemplate;
        this.opaEnabled = opaEnabled;
        this.opaUrl = opaUrl;
        this.userRoleRepo = tenantUserRepository;
        this.redis = redis;
        this.cacheTtl = Duration.ofSeconds(ttlSeconds);
    }

    private String cacheKey(String externalId) {
        return "perm:tenants:" + externalId;
    }

    @Override
    public List<String> getTenantsForExternalId(String externalId) {
        String key = cacheKey(externalId);
        String cached = redis.opsForValue().get(key);
        if (cached != null && !cached.isEmpty()) {
            // cached as CSV
            return Arrays.stream(cached.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        List<UserRoleBindingViewEntity> rows = userRoleRepo.findByExternalId(externalId);
        List<String> tenantIds = rows.stream()
                .map(UserRoleBindingViewEntity::getScopeId)
                .filter(Objects::nonNull)
                .map(UUID::toString)
                .distinct()
                .collect(Collectors.toList());

        String csv = String.join(",", tenantIds);
        if (!csv.isEmpty()) {
            redis.opsForValue().set(key, csv, cacheTtl);
        } else {
            // negative cache to avoid DB spam; short TTL
            redis.opsForValue().set(key, "", Duration.ofSeconds(Math.min(cacheTtl.getSeconds(), 60)));
        }

        return tenantIds;
    }

    @Override
    public boolean hasAccessToTenant(String externalId, String tenantId) {
        List<String> tenants = getTenantsForExternalId(externalId);
        return tenants.contains(tenantId);
    }

    @Override
    public void evictCacheForExternalId(String externalId) {
        redis.delete(cacheKey(externalId));
    }

    // cache user permissions via Spring Cache and Redis
    @Cacheable(cacheNames = "userPermissions", key = "#user.id")
    public List<String> getCachedPermissions(UserPrincipal user) {
        // return list of permission strings like "vm:create", "vm:*", "admin:*"
        return List.of();//permissionRepository.getPermissionsForUser(user.getId());
    }

    @Override
    public boolean isAllowed(UserPrincipal user, String action, Scope resourceScope, Object resource) {
        // 1) fetch cached perms
        List<String> perms = getCachedPermissions(user);

        // 2) quick allow if any permission matches action or has wildcard
        boolean roleGrant = perms.stream().anyMatch(p -> matchesPermissionPattern(p, action));
        if (roleGrant && !opaEnabled) {
            return true;
        }

        // 3) if ABAC enabled (OPA) - evaluate policies (OPA explicit DENY -> deny)
        if (opaEnabled) {
            Boolean opaDecision = callOpa(user, action, resource, resourceScope);
            // OPA explicit deny (false) overrides role grants
            if (opaDecision != null && !opaDecision) return false;
            // If OPA said allow -> allow (role grants + ABAC allow => allow)
            if (opaDecision != null && opaDecision) {
                return true;
            }
            // opaDecision null => fallback to roleGrant
        }

        // 4) fallback: allow only if roleGrant true
        return roleGrant;
    }

    private boolean matchesPermissionPattern(String pattern, String action) {
        // pattern may contain wildcard '*'
        // convert to regex: escape regex chars except *, replace * -> .*
        String escaped = Pattern.quote(pattern).replace("\\*", ".*");
        return Pattern.compile("^" + escaped + "$").matcher(action).matches();
    }

    private Boolean callOpa(UserPrincipal user, String action, Object resource, Scope scope) {
        try {
            Map<String,Object> input = new HashMap<>();
            input.put("user", Map.of("id", user.id(), "roles", user.roles()/*, "attrs", user.attrs()*/));
            input.put("action", action);
            input.put("resource", resource == null ? Map.of("scope", scope) : resource);
            Map<String,Object> body = Map.of("input", input);
            // expect OPA to return {"result": {"allow": true}} or {"result": {"allow": false}}
            Map resp = restTemplate.postForObject(opaUrl, body, Map.class);
            if (resp == null) return null;
            Object r = resp.get("result");
            if (r instanceof Map) {
                Object allow = ((Map) r).get("allow");
                if (allow instanceof Boolean) return (Boolean) allow;
            }
        } catch (Exception ex) {
            // log, but do not fail open blindly. Here we prefer best-effort: return null to fallback on roleGrant.
            // In higher-security setups, you may want to deny if OPA is down.
        }
        return null;
    }
}
