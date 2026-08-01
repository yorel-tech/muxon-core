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
package com.yorel.muxon.hateoas;

import com.yorel.muxon.api.model.Provider;
import com.yorel.muxon.auth.Permission;
import com.yorel.muxon.auth.ResourceAction;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Registers HATEOAS action descriptors for Provider resources when they are not discovered via
 * {@link ResourceAction} annotations (e.g. when mappings are on the OpenAPI-generated interface).
 */
@Configuration
public class ProviderLinkRegistrationConfig {

  private static final String PROVIDER_BASE = "/api/v1/providers";
  private static final String PROVIDER_ID_PATH = PROVIDER_BASE + "/{providerId}";

  private final ResourceActionRegistry resourceActionRegistry;

  public ProviderLinkRegistrationConfig(ResourceActionRegistry resourceActionRegistry) {
    this.resourceActionRegistry = resourceActionRegistry;
  }

  @PostConstruct
  public void registerProviderActions() {
    if (!resourceActionRegistry.getActionsFor(Provider.class).isEmpty()) {
      return; // already discovered via annotations
    }
    List<ResourceActionDescriptor> descriptors =
        List.of(
            new ResourceActionDescriptor(
                "self",
                "",
                RequestMethod.GET,
                Provider.class,
                Permission.PROVIDER_READ,
                "providerId",
                "",
                PROVIDER_ID_PATH),
            new ResourceActionDescriptor(
                "edit",
                "Replace provider",
                RequestMethod.PUT,
                Provider.class,
                Permission.PROVIDER_EDIT,
                "providerId",
                "",
                PROVIDER_ID_PATH),
            new ResourceActionDescriptor(
                "update",
                "Update provider",
                RequestMethod.PATCH,
                Provider.class,
                Permission.PROVIDER_EDIT,
                "providerId",
                "",
                PROVIDER_ID_PATH),
            new ResourceActionDescriptor(
                "delete",
                "Delete provider",
                RequestMethod.DELETE,
                Provider.class,
                Permission.PROVIDER_MANAGE,
                "providerId",
                "",
                PROVIDER_ID_PATH),
            new ResourceActionDescriptor(
                "test",
                "Test connection",
                RequestMethod.POST,
                Provider.class,
                Permission.PROVIDER_READ,
                "providerId",
                "",
                PROVIDER_ID_PATH + "/test"),
            new ResourceActionDescriptor(
                "capabilities",
                "Get capabilities",
                RequestMethod.GET,
                Provider.class,
                Permission.PROVIDER_READ,
                "providerId",
                "",
                PROVIDER_ID_PATH + "/capabilities"));
    resourceActionRegistry.registerActions(Provider.class, descriptors);
  }
}
