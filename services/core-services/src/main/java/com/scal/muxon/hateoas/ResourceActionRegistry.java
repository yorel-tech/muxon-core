package com.scal.muxon.hateoas;

import com.scal.muxon.auth.ResourceAction;
import org.springframework.beans.factory.annotation.Qualifier;
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

    public ResourceActionRegistry(@Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
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
            Optional<String> pathTemplate = Optional.ofNullable(mappingInfo.getPathPatternsCondition())
                    .flatMap(pc -> pc.getPatterns().stream().findFirst())
                    .map(Object::toString);

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

    /**
     * Register action descriptors for a resource type programmatically.
     * Use when annotations are on an interface (e.g. OpenAPI-generated API) and not visible on the handler method.
     */
    public void registerActions(Class<?> resourceType, List<ResourceActionDescriptor> descriptors) {
        actionsByResourceType.merge(resourceType, new ArrayList<>(descriptors), (existing, added) -> {
            List<ResourceActionDescriptor> merged = new ArrayList<>(existing);
            merged.addAll(added);
            return merged;
        });
    }
}

