package com.onetattva.infron.core.auth;

import org.springframework.web.bind.annotation.RequestMethod;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative description of a HATEOAS action associated with a controller method.
 * <p>
 * This annotation is intended to be placed on Spring MVC handler methods that
 * represent actions on a specific resource type (e.g. Provider, Node).
 * At runtime, a registry can scan these annotations and use them to
 * automatically generate {@code Link} objects.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResourceAction {

    /**
     * Relation type for the link (e.g. "self", "edit", "delete", "sync").
     */
    String rel();

    /**
     * Human-readable title for the action.
     * If left empty, a sensible default can be derived from {@link #rel()}.
     */
    String title() default "";

    /**
     * HTTP method to use for the action link.
     */
    RequestMethod method();

    /**
     * Resource type this action operates on (API model or entity class).
     */
    Class<?> resourceType();

    /**
     * Permission to check for this action.
     * If not set explicitly, callers should treat the action as requiring
     * {@link Permission#SYSTEM_SETTINGS} or override this behavior in their logic.
     */
    Permission permission() default Permission.SYSTEM_SETTINGS;

    /**
     * Name of the path variable in the handler method's mapping that contains
     * the resource identifier (e.g. "providerId", "nodeId").
     */
    String idParam() default "id";

    /**
     * Optional SpEL condition that must evaluate to {@code true} for the
     * action to be enabled. The expression can reference {@code #resource}
     * (the resource instance) and {@code #user} (the current principal).
     */
    String condition() default "";
}

