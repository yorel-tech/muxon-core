package com.onetattva.infron.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private static final String PATTERN_V1 = "/api/v1/tenants/{tenantId}/**";
    private static final String PATTERN_LEGACY = "/api/tenant/{tenantId}/**";

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true; // not a method handler, allow
        }

        RequiresPermission annotation = handlerMethod.getMethodAnnotation(RequiresPermission.class);
        if (annotation == null) {
            // Check interface methods if annotation not found on implementation
            Class<?>[] interfaces = handlerMethod.getMethod().getDeclaringClass().getInterfaces();
            for (Class<?> interfaceClass : interfaces) {
                try {
                    java.lang.reflect.Method interfaceMethod = interfaceClass.getMethod(handlerMethod.getMethod().getName(), handlerMethod.getMethod().getParameterTypes());
                    annotation = interfaceMethod.getAnnotation(RequiresPermission.class);
                    if (annotation != null) {
                        break;
                    }
                } catch (NoSuchMethodException | SecurityException e) {
                    // Continue checking other interfaces
                }
            }
        }
        if (annotation == null) {
            return true; // no permission required
        }

        Permission permission = annotation.value();
        String action = permission.getAction();

        // Get user from SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return false;
        }

        String path = request.getRequestURI();
        String tenantId = extractTenantIdFromPath(path);

        boolean allowed;
        if (tenantId != null) {
            allowed = authorizationService.isAllowedForTenant(user, action, tenantId);
        } else {
            allowed = authorizationService.isAllowed(user, action, Scope.SYSTEM, null);
        }
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
            return false;
        }

        return true;
    }

    /**
     * If the path is a tenant-scoped path (/api/v1/tenants/{tenantId}/... or /api/tenant/{tenantId}/...),
     * returns the tenantId segment; otherwise returns null.
     */
    private String extractTenantIdFromPath(String path) {
        if (path == null) return null;
        if (path.equals("/api/v1/tenants") || path.startsWith("/api/v1/tenants/current") || path.startsWith("/api/v1/tenants?")) {
            return null;
        }
        if (pathMatcher.match(PATTERN_V1, path)) {
            String[] parts = path.split("/");
            return parts.length >= 5 ? parts[4] : null;
        }
        if (pathMatcher.match(PATTERN_LEGACY, path)) {
            String[] parts = path.split("/");
            return parts.length >= 4 ? parts[3] : null;
        }
        return null;
    }
}
