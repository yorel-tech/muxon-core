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
package com.yorel.muxon.web.exceptions;

import java.util.UUID;

/** Exception thrown when a requested resource is not found. */
public class ResourceNotFoundException extends RuntimeException {

  private final UUID resourceId;
  private final String resourceType;

  public ResourceNotFoundException(UUID resourceId, String resourceType) {
    super(String.format("%s not found with id: %s", resourceType, resourceId));
    this.resourceId = resourceId;
    this.resourceType = resourceType;
  }

  public ResourceNotFoundException(String resourceType, String identifier, String value) {
    super(String.format("%s not found with %s: %s", resourceType, identifier, value));
    this.resourceId = null;
    this.resourceType = resourceType;
  }

  public ResourceNotFoundException(String message) {
    super(message);
    this.resourceId = null;
    this.resourceType = null;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }
}
