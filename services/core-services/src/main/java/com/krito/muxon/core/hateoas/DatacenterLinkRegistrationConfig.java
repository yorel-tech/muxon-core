package com.krito.muxon.core.hateoas;

import com.krito.muxon.api.model.Datacenter;
import com.krito.muxon.core.auth.Permission;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Registers HATEOAS action descriptors for Datacenter resources.
 */
@Configuration
public class DatacenterLinkRegistrationConfig {

    private static final String DATACENTER_BASE = "/api/v1/datacenters";
    private static final String DATACENTER_ID_PATH = DATACENTER_BASE + "/{datacenterId}";

    private final ResourceActionRegistry resourceActionRegistry;

    public DatacenterLinkRegistrationConfig(ResourceActionRegistry resourceActionRegistry) {
        this.resourceActionRegistry = resourceActionRegistry;
    }

    @PostConstruct
    public void registerDatacenterActions() {
        if (!resourceActionRegistry.getActionsFor(Datacenter.class).isEmpty()) {
            return;
        }
        List<ResourceActionDescriptor> descriptors = List.of(
                new ResourceActionDescriptor("self", "", RequestMethod.GET, Datacenter.class, Permission.DATACENTER_READ, "datacenterId", "", DATACENTER_ID_PATH),
                new ResourceActionDescriptor("edit", "Replace datacenter", RequestMethod.PUT, Datacenter.class, Permission.DATACENTER_EDIT, "datacenterId", "", DATACENTER_ID_PATH),
                new ResourceActionDescriptor("update", "Update datacenter", RequestMethod.PATCH, Datacenter.class, Permission.DATACENTER_EDIT, "datacenterId", "", DATACENTER_ID_PATH),
                new ResourceActionDescriptor("delete", "Delete datacenter", RequestMethod.DELETE, Datacenter.class, Permission.DATACENTER_MANAGE, "datacenterId", "", DATACENTER_ID_PATH),
                new ResourceActionDescriptor("settings", "Get datacenter settings", RequestMethod.GET, Datacenter.class, Permission.DATACENTER_READ, "datacenterId", "", DATACENTER_ID_PATH + "/settings"),
                new ResourceActionDescriptor("replaceSettings", "Update datacenter settings", RequestMethod.PUT, Datacenter.class, Permission.DATACENTER_EDIT, "datacenterId", "", DATACENTER_ID_PATH + "/settings"),
                new ResourceActionDescriptor("metadata", "Get datacenter metadata", RequestMethod.GET, Datacenter.class, Permission.DATACENTER_READ, "datacenterId", "", DATACENTER_ID_PATH + "/metadata"),
                new ResourceActionDescriptor("updateMetadata", "Update datacenter metadata", RequestMethod.PUT, Datacenter.class, Permission.DATACENTER_EDIT, "datacenterId", "", DATACENTER_ID_PATH + "/metadata")
        );
        resourceActionRegistry.registerActions(Datacenter.class, descriptors);
    }
}
