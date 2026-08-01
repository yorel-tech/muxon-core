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
package com.yorel.muxon.db.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Entity for OIDC identity providers. */
@Entity
@DiscriminatorValue("OIDC")
public class OidcIdentityProviderEntity extends IdentityProviderEntity {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // Constructors
  public OidcIdentityProviderEntity() {
    super();
    setProtocol(IdentityProviderProtocol.OIDC);
  }

  /**
   * Gets the OIDC metadata.
   *
   * @return the OIDC metadata
   */
  public OidcMetadata getOidcMetadata() {
    Map<String, String> metadata = getMetadata();
    if (metadata == null) {
      return null;
    }
    return objectMapper.convertValue(metadata, OidcMetadata.class);
  }

  /**
   * Sets the OIDC metadata.
   *
   * @param oidcMetadata the OIDC metadata
   */
  public void setOidcMetadata(OidcMetadata oidcMetadata) {
    if (oidcMetadata == null) {
      setMetadata(null);
      return;
    }
    Map<String, String> metadata =
        objectMapper.convertValue(oidcMetadata, new TypeReference<Map<String, String>>() {});
    setMetadata(metadata);
  }
}
