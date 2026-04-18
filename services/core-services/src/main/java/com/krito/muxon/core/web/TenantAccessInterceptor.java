package com.krito.muxon.core.web;


import com.krito.muxon.core.auth.AuthorizationService;
import com.krito.muxon.core.auth.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.AntPathMatcher;

import java.nio.charset.StandardCharsets;

@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    private final AuthorizationService authzService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantAccessInterceptor(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    private static final String PATTERN_V1 = "/api/v1/tenants/{tenantId}/**";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI();
        // Skip paths that do not contain a tenant id (list and current-tenant endpoint)
        if (path.equals("/api/v1/tenants") || path.startsWith("/api/v1/tenants/current") || path.startsWith("/api/v1/tenants?")) {
            return true;
        }

        boolean v1Match = pathMatcher.match(PATTERN_V1, path);
        if (!v1Match) {
            return true; // not a tenant-scoped endpoint
        }

        String[] parts = path.split("/");
        String tenantId;
        // expected: ["", "api", "v1", "tenants", "{tenantId}", ...]
        if (parts.length < 5) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed tenant path");
            return false;
        }
        // /api/v1/tenants/{id} only: system tenant APIs; membership check is for sub-resources.
        if (parts.length == 5) {
            return true;
        }
        tenantId = parts[4];

        // Extract external user id from security context (UserPrincipal or Jwt)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthenticated");
            return false;
        }
        String externalId = null;
        if (auth.getPrincipal() instanceof UserPrincipal user) {
            externalId = user.id();
        } else if (auth.getPrincipal() instanceof Jwt jwt) {
            externalId = jwt.getSubject();
        }
        if (externalId == null || externalId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing subject");
            return false;
        }

        boolean allowed = authzService.hasAccessToTenant(externalId, tenantId);
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"INVALID_TENANT_CONTEXT\"}");
            response.getWriter().flush();
            return false;
        }
        // permitted
        return true;
    }
}
