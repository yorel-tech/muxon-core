package com.onetattva.infron.core.web;

import com.onetattva.infron.api.model.Datacenter;
import com.onetattva.infron.api.model.DatacenterList;
import com.onetattva.infron.api.model.Provider;
import com.onetattva.infron.api.model.ProviderList;
import com.onetattva.infron.api.model.Tenant;
import com.onetattva.infron.api.model.TenantList;
import com.onetattva.infron.core.hateoas.ActionLinkService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Automatically enriches API responses with HATEOAS links.
 * Controllers return plain service results; this advice adds _links before serialization.
 */
@ControllerAdvice
public class HateoasLinkEnrichmentAdvice implements ResponseBodyAdvice<Object> {

    private final ActionLinkService actionLinkService;

    public HateoasLinkEnrichmentAdvice(ActionLinkService actionLinkService) {
        this.actionLinkService = actionLinkService;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getContainingClass().isAnnotationPresent(RestController.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                 MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                 ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        if (body instanceof Provider provider) {
            provider.setLinks(actionLinkService.generateProviderLinksById(provider.getId()));
        } else if (body instanceof ProviderList providerList && providerList.getItems() != null) {
            providerList.getItems().forEach(provider ->
                    provider.setLinks(actionLinkService.generateProviderLinksById(provider.getId())));
        } else if (body instanceof Datacenter datacenter && datacenter.getId() != null) {
            datacenter.setLinks(actionLinkService.generateDatacenterLinksById(datacenter.getId()));
        } else if (body instanceof DatacenterList datacenterList && datacenterList.getItems() != null) {
            datacenterList.getItems().forEach(dc ->
                    dc.setLinks(actionLinkService.generateDatacenterLinksById(dc.getId())));
        } else if (body instanceof Tenant tenant && tenant.getId() != null) {
            tenant.setLinks(actionLinkService.generateTenantLinksById(tenant.getId()));
        } else if (body instanceof TenantList tenantList && tenantList.getItems() != null) {
            tenantList.getItems().forEach(t ->
                    t.setLinks(actionLinkService.generateTenantLinksById(t.getId())));
        }
        return body;
    }
}
