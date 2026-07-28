package com.scal.muxon.hateoas;

import com.scal.muxon.api.model.Datacenter;
import com.scal.muxon.api.model.Link;
import com.scal.muxon.api.model.Node;
import com.scal.muxon.api.model.NodeCluster;
import com.scal.muxon.api.model.Provider;
import com.scal.muxon.api.model.Tenant;
import com.scal.muxon.api.util.LinkUtil;
import com.scal.muxon.auth.AuthorizationService;
import com.scal.muxon.auth.Permission;
import com.scal.muxon.auth.Scope;
import com.scal.muxon.auth.UserPrincipal;
import com.scal.muxon.services.NodeClustersService;
import com.scal.muxon.services.NodesService;
import com.scal.muxon.services.ProvidersService;
import com.scal.muxon.db.model.DatacenterEntity;
import com.scal.muxon.db.model.NodeClusterEntity;
import com.scal.muxon.db.model.NodeEntity;
import com.scal.muxon.db.model.ProviderEntity;
import com.scal.muxon.db.model.TenantEntity;
import com.scal.muxon.db.repository.DatacenterRepository;
import com.scal.muxon.db.repository.TenantRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ActionLinkService {

    private final AuthorizationService authorizationService;
    private final ProvidersService providersService;
    private final NodeClustersService nodeClustersService;
    private final NodesService nodesService;
    private final DatacenterRepository datacenterRepository;
    private final TenantRepository tenantRepository;
    private final ResourceActionRegistry resourceActionRegistry;

    public ActionLinkService(AuthorizationService authorizationService,
                             ProvidersService providersService,
                             NodeClustersService nodeClustersService,
                             NodesService nodesService,
                             DatacenterRepository datacenterRepository,
                             TenantRepository tenantRepository,
                             ResourceActionRegistry resourceActionRegistry) {
        this.authorizationService = authorizationService;
        this.providersService = providersService;
        this.nodeClustersService = nodeClustersService;
        this.nodesService = nodesService;
        this.datacenterRepository = datacenterRepository;
        this.tenantRepository = tenantRepository;
        this.resourceActionRegistry = resourceActionRegistry;
    }
    
    /**
     * Get the current authenticated user from SecurityContext.
     * Public so SpEL in @Cacheable key expressions can call it on the proxy.
     * @return UserPrincipal or null if not authenticated
     */
    public UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        return null;
    }
    
    @Cacheable(value = "actionLinks", key = "'provider-links-' + #provider.getId() + '-' + @authorizationService.getCachedPermissions(@actionLinkService.getCurrentUser())")
    public List<Link> generateProviderLinks(ProviderEntity provider) {
        UserPrincipal user = getCurrentUser();
        return buildLinksForResource(Provider.class, provider.getId().toString(), provider, user);
    }

    /**
     * Convenience overload to generate provider links from provider ID.
     */
    public List<Link> generateProviderLinksById(UUID providerId) {
        ProviderEntity provider = providersService.getProviderEntity(providerId);
        return generateProviderLinks(provider);
    }
    
    private boolean isAllowed(UserPrincipal user, String action) {
        if (user == null) {
            return false;
        }
        return authorizationService.isAllowed(user, action, Scope.SYSTEM, null);
    }

    @Cacheable(value = "actionLinks", key = "'cluster-links-' + #cluster.getId() + '-' + @authorizationService.getCachedPermissions(@actionLinkService.getCurrentUser())")
    public List<Link> generateClusterLinks(NodeClusterEntity cluster) {
        UserPrincipal user = getCurrentUser();
        return buildLinksForResource(NodeCluster.class, cluster.getId().toString(), cluster, user);
    }
    
    @Cacheable(value = "actionLinks", key = "'node-links-' + #node.getId() + '-' + @authorizationService.getCachedPermissions(@actionLinkService.getCurrentUser())")
    public List<Link> generateNodeLinks(NodeEntity node) {
        UserPrincipal user = getCurrentUser();
        return buildLinksForResource(Node.class, node.getId().toString(), node, user);
    }

    @Cacheable(value = "actionLinks", key = "'datacenter-links-' + #datacenterId + '-' + @authorizationService.getCachedPermissions(@actionLinkService.getCurrentUser())")
    public List<Link> generateDatacenterLinksById(UUID datacenterId) {
        UserPrincipal user = getCurrentUser();
        DatacenterEntity entity = datacenterRepository.findById(datacenterId).orElse(null);
        return buildLinksForResource(Datacenter.class, datacenterId.toString(), entity, user);
    }

    @Cacheable(value = "actionLinks", key = "'tenant-links-' + #tenantId + '-' + @authorizationService.getCachedPermissions(@actionLinkService.getCurrentUser())")
    public List<Link> generateTenantLinksById(UUID tenantId) {
        UserPrincipal user = getCurrentUser();
        TenantEntity entity = tenantRepository.findById(tenantId).orElse(null);
        return buildLinksForResource(Tenant.class, tenantId.toString(), entity, user);
    }

    private List<Link> buildLinksForResource(Class<?> resourceType,
                                             String resourceId,
                                             Object resource,
                                             UserPrincipal user) {
        List<Link> links = new ArrayList<>();

        for (ResourceActionDescriptor descriptor : resourceActionRegistry.getActionsFor(resourceType)) {
            // Build href from path template by replacing the id parameter placeholder.
            String href = descriptor.pathTemplate()
                    .replace("{" + descriptor.idParam() + "}", resourceId);

            // Self link is always enabled and does not perform permission checks.
            if ("self".equals(descriptor.rel())) {
                links.add(LinkUtil.self(href));
                continue;
            }

            boolean enabled = true;
            String reason = null;

            Permission permission = descriptor.permission();
            if (permission != null) {
                String action = permission.getAction();
                enabled = isAllowed(user, action);
                if (!enabled) {
                    reason = "Missing " + action + " permission";
                }
            }

            // Apply additional entity-specific rules.
            if (resourceType.equals(Provider.class) && resource instanceof ProviderEntity providerEntity) {
                if ("delete".equals(descriptor.rel()) && enabled && providerEntity.hasActiveResources()) {
                    enabled = false;
                    reason = "Provider has active resources or missing permission";
                }
                if ("sync".equals(descriptor.rel()) && enabled && "OFFLINE".equals(providerEntity.getStatus())) {
                    enabled = false;
                    reason = "Provider is offline or missing sync permission";
                }
            } else if (resourceType.equals(NodeCluster.class) && resource instanceof NodeClusterEntity clusterEntity) {
                if ("delete".equals(descriptor.rel()) && enabled && clusterEntity.hasActiveNodes()) {
                    enabled = false;
                    reason = "Cluster has active nodes or missing permission";
                }
            } else if (resourceType.equals(Datacenter.class) && resource instanceof DatacenterEntity) {
                // Datacenter delete: could disable when tenant grants exist; currently no check
            } else if (resourceType.equals(Tenant.class) && resource instanceof TenantEntity) {
                // Tenant: no entity-specific rules for now
            }

            String title = descriptor.title();
            if (title == null || title.isEmpty()) {
                title = capitalize(descriptor.rel());
            }

            HttpMethod httpMethod = HttpMethod.valueOf(descriptor.method().name());

            Link link;
            switch (descriptor.rel()) {
                case "edit":
                    link = LinkUtil.edit(href, enabled);
                    break;
                case "delete":
                    link = LinkUtil.delete(href, enabled, reason);
                    break;
                default:
                    link = LinkUtil.custom(
                            descriptor.rel(),
                            href,
                            httpMethod.name(),
                            title,
                            enabled,
                            reason
                    );
                    break;
            }

            links.add(link);
        }

        return links;
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }
}
