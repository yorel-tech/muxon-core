package com.yorel.muxon.hateoas;

import com.yorel.muxon.api.model.Tenant;
import com.yorel.muxon.auth.Permission;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Registers HATEOAS action descriptors for Tenant resources.
 * Aligns with UI tenant page: View Details, Edit (description/display name only), Delete.
 * Renaming (name/slug) is not allowed; only PATCH (update) is exposed for editing.
 */
@Configuration
public class TenantLinkRegistrationConfig {

    private static final String TENANTS_BASE = "/api/v1/tenants";
    private static final String TENANT_ID_PATH = TENANTS_BASE + "/{tenantId}";

    private final ResourceActionRegistry resourceActionRegistry;

    public TenantLinkRegistrationConfig(ResourceActionRegistry resourceActionRegistry) {
        this.resourceActionRegistry = resourceActionRegistry;
    }

    @PostConstruct
    public void registerTenantActions() {
        if (!resourceActionRegistry.getActionsFor(Tenant.class).isEmpty()) {
            return;
        }
        List<ResourceActionDescriptor> descriptors = List.of(
                new ResourceActionDescriptor("self", "View Details", RequestMethod.GET, Tenant.class, Permission.TENANT_READ, "tenantId", "", TENANT_ID_PATH),
                new ResourceActionDescriptor("update", "Edit", RequestMethod.PATCH, Tenant.class, Permission.TENANT_EDIT, "tenantId", "", TENANT_ID_PATH),
                new ResourceActionDescriptor("delete", "Delete", RequestMethod.DELETE, Tenant.class, Permission.TENANT_MANAGE, "tenantId", "", TENANT_ID_PATH)
        );
        resourceActionRegistry.registerActions(Tenant.class, descriptors);
    }
}
