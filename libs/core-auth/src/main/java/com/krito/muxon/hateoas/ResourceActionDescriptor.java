package com.krito.muxon.hateoas;

import com.krito.muxon.auth.Permission;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Immutable description of a single HATEOAS action declared via {@link com.krito.muxon.auth.ResourceAction}.
 */
public record ResourceActionDescriptor(
        String rel,
        String title,
        RequestMethod method,
        Class<?> resourceType,
        Permission permission,
        String idParam,
        String condition,
        String pathTemplate
) {
}

