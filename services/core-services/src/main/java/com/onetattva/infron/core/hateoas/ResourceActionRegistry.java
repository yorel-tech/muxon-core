package com.onetattva.infron.core.hateoas;

import com.onetattva.infron.core.auth.ResourceAction;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that discovers {@link ResourceAction} annotations on Spring MVC
 * handler methods and exposes them as descriptors grouped by resource type.
 */
@Component
public class ResourceActionRegistry {

    private final Map<Class<?>, List<ResourceActionDescriptor>> actionsByResourceType = new ConcurrentHashMap<>();

    public ResourceActionRegistry(RequestMappingHandlerMapping handlerMapping) {
        initialize(handlerMapping);
    }

    private void initialize(RequestMappingHandlerMapping handlerMapping) {
        Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            ResourceAction resourceAction = handlerMethod.getMethodAnnotation(ResourceAction.class);
            if (resourceAction == null) {
                continue;
            }

            RequestMappingInfo mappingInfo = entry.getKey();
            Optional<String> pathTemplate = mappingInfo.getPathPatternsCondition() != null
                    ? mappingInfo.getPathPatternsCondition().getPatterns().stream().findFirst().map(Object::toString)
                    : mappingInfo.getPatternsCondition().getPatterns().stream().findFirst();

            if (pathTemplate.isEmpty()) {
                continue;
            }

            ResourceActionDescriptor descriptor = new ResourceActionDescriptor(
                    resourceAction.rel(),
                    resourceAction.title(),
                    resourceAction.method(),
                    resourceAction.resourceType(),
                    resourceAction.permission(),
                    resourceAction.idParam(),
                    resourceAction.condition(),
                    pathTemplate.get()
            );

            actionsByResourceType
                    .computeIfAbsent(resourceAction.resourceType(), _ -> new ArrayList<>())
                    .add(descriptor);
        }
    }

    /**
     * Get all action descriptors for a given resource type.
     */
    public List<ResourceActionDescriptor> getActionsFor(Class<?> resourceType) {
        return actionsByResourceType.getOrDefault(resourceType, Collections.emptyList());
    }
}

