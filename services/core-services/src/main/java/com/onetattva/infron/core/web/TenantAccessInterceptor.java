package com.onetattva.infron.core.web;


import com.onetattva.infron.core.auth.AuthorizationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.util.AntPathMatcher;

@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    private final AuthorizationService authzService;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public TenantAccessInterceptor(AuthorizationService authzService) {
        this.authzService = authzService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String path = request.getRequestURI();
        // Pattern: /api/tenant/{tenantId}/**
        String pattern = "/api/tenant/{tenantId}/**";
        if (!pathMatcher.match(pattern, path)) {
            return true; // not a tenant-scoped endpoint
        }

        // extract tenantId
        String[] parts = path.split("/");
        // expected: ["", "api","tenant","{tenantId}", ...]
        if (parts.length < 4) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Malformed tenant path");
            return false;
        }
        String tenantId = parts[3];

        // extract jwt subject from security context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthenticated");
            return false;
        }
        Jwt jwt = (Jwt) auth.getPrincipal();
        String externalId = jwt.getSubject();
        if (externalId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing subject");
            return false;
        }

        boolean allowed = authzService.hasAccessToTenant(externalId, tenantId);
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access to tenant denied");
            return false;
        }
        // permitted
        return true;
    }
}
