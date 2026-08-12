/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
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

  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  @Autowired private AuthorizationService authorizationService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true; // not a method handler, allow
    }

    RequiresAnyPermission anyPerm =
        findMethodAnnotation(handlerMethod, RequiresAnyPermission.class);
    RequiresPermission singlePerm =
        anyPerm == null ? findMethodAnnotation(handlerMethod, RequiresPermission.class) : null;

    if (anyPerm == null && singlePerm == null) {
      return true; // no permission required
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
      return false;
    }

    String path = request.getRequestURI();
    String tenantId = extractTenantIdFromPath(path);

    boolean allowed;
    if (anyPerm != null) {
      allowed = false;
      for (Permission p : anyPerm.value()) {
        if (isActionAllowed(user, p.getAction(), tenantId)) {
          allowed = true;
          break;
        }
      }
    } else {
      allowed = isActionAllowed(user, singlePerm.value().getAction(), tenantId);
    }

    if (!allowed) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
      return false;
    }

    return true;
  }

  private boolean isActionAllowed(UserPrincipal user, String action, String tenantId) {
    if (tenantId != null) {
      return authorizationService.isAllowedForTenant(user, action, tenantId);
    }
    return authorizationService.isAllowed(user, action, Scope.SYSTEM, null);
  }

  private <A extends java.lang.annotation.Annotation> A findMethodAnnotation(
      HandlerMethod handlerMethod, Class<A> ann) {
    A a = handlerMethod.getMethodAnnotation(ann);
    if (a != null) {
      return a;
    }
    Class<?>[] interfaces = handlerMethod.getMethod().getDeclaringClass().getInterfaces();
    for (Class<?> interfaceClass : interfaces) {
      try {
        Method interfaceMethod =
            interfaceClass.getMethod(
                handlerMethod.getMethod().getName(), handlerMethod.getMethod().getParameterTypes());
        a = interfaceMethod.getAnnotation(ann);
        if (a != null) {
          return a;
        }
      } catch (NoSuchMethodException | SecurityException e) {
        // continue
      }
    }
    return null;
  }

  /**
   * If the path is a tenant-scoped path (/api/v1/tenants/{tenantId}/... or
   * /api/tenant/{tenantId}/...), returns the tenantId segment; otherwise returns null.
   */
  private String extractTenantIdFromPath(String path) {
    if (path == null) return null;
    if (path.equals("/api/v1/tenants")
        || path.startsWith("/api/v1/tenants/current")
        || path.startsWith("/api/v1/tenants?")) {
      return null;
    }
    if (pathMatcher.match(PATTERN_V1, path)) {
      String[] parts = path.split("/");
      // Exactly /api/v1/tenants/{tenantId} (tenant CRUD) uses system-scoped permissions;
      // do not treat as tenant-scoped path (would check only TENANT role bindings).
      if (parts.length == 5) {
        return null;
      }
      return parts.length >= 5 ? parts[4] : null;
    }
    return null;
  }
}
