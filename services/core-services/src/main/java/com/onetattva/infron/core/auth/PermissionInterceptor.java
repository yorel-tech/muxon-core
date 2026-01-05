package com.onetattva.infron.core.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

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

        // Check permission
        boolean allowed = authorizationService.isAllowed(user, action, Scope.SYSTEM, null); // using SYSTEM as default scope
        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
            return false;
        }

        return true;
    }
}
