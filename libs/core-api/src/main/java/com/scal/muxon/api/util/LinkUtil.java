package com.scal.muxon.api.util;

import com.scal.muxon.api.model.Link;

/**
 * Utility class for creating HATEOAS Link instances.
 * Provides factory methods for common link types.
 */
public final class LinkUtil {

    private LinkUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Creates a "self" link for viewing details.
     *
     * @param href the URL for the self link
     * @return a Link with rel="self", method=GET
     */
    public static Link self(String href) {
        return new Link()
                .rel("self")
                .href(href)
                .method(Link.MethodEnum.GET)
                .title("View Details")
                .enabled(true);
    }

    /**
     * Creates an "edit" link for modifying resources.
     *
     * @param href the URL for the edit link
     * @param enabled whether the edit action is enabled
     * @return a Link with rel="edit", method=PUT
     */
    public static Link edit(String href, boolean enabled) {
        return new Link()
                .rel("edit")
                .href(href)
                .method(Link.MethodEnum.PUT)
                .title("Edit")
                .enabled(enabled)
                .reason(enabled ? null : "Missing edit permission");
    }

    /**
     * Creates a "delete" link for removing resources.
     *
     * @param href the URL for the delete link
     * @param enabled whether the delete action is enabled
     * @param reason the reason if the action is disabled
     * @return a Link with rel="delete", method=DELETE
     */
    public static Link delete(String href, boolean enabled, String reason) {
        return new Link()
                .rel("delete")
                .href(href)
                .method(Link.MethodEnum.DELETE)
                .title("Delete")
                .enabled(enabled)
                .reason(reason);
    }

    /**
     * Creates a custom link with full control over all properties.
     *
     * @param rel the relation type
     * @param href the URL for the link
     * @param method the HTTP method
     * @param title the human-readable title
     * @param enabled whether the action is enabled
     * @param reason the reason if the action is disabled
     * @return a Link with the specified properties
     */
    public static Link custom(String rel, String href, String method, String title, boolean enabled, String reason) {
        return new Link()
                .rel(rel)
                .href(href)
                .method(Link.MethodEnum.fromValue(method))
                .title(title)
                .enabled(enabled)
                .reason(reason);
    }
}
