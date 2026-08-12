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
import jakarta.persistence.Transient;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/** Entity for SAML 2.0 identity providers. */
@Entity
@DiscriminatorValue("SAML2")
public class Saml2IdentityProviderEntity extends IdentityProviderEntity {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  // Constructors
  public Saml2IdentityProviderEntity() {
    super();
    setProtocol(IdentityProviderProtocol.SAML2);
  }

  /**
   * Gets the SAML 2.0 metadata.
   *
   * @return the SAML 2.0 metadata
   */
  @Transient
  public Saml2Metadata getSaml2Metadata() {
    Map<String, String> metadata = getMetadata();
    if (metadata == null) {
      return null;
    }
    return objectMapper.convertValue(metadata, Saml2Metadata.class);
  }

  /**
   * Sets the SAML 2.0 metadata.
   *
   * @param saml2Metadata the SAML 2.0 metadata
   */
  public void setSaml2Metadata(Saml2Metadata saml2Metadata) {
    if (saml2Metadata == null) {
      setMetadata(null);
      return;
    }
    Map<String, String> metadata =
        objectMapper.convertValue(saml2Metadata, new TypeReference<Map<String, String>>() {});
    setMetadata(metadata);
  }
}
