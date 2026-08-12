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

/** Exception thrown when a user lacks permission to perform an action on a resource. */
public class PermissionDeniedException extends RuntimeException {

  private final String permission;
  private final UUID resourceId;
  private final String resourceType;

  public PermissionDeniedException(String permission, UUID resourceId, String resourceType) {
    super(
        String.format(
            "Permission denied: %s on %s with id %s", permission, resourceType, resourceId));
    this.permission = permission;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
  }

  public PermissionDeniedException(String permission, String resourceType) {
    super(String.format("Permission denied: %s on %s", permission, resourceType));
    this.permission = permission;
    this.resourceId = null;
    this.resourceType = resourceType;
  }

  public PermissionDeniedException(String message) {
    super(message);
    this.permission = null;
    this.resourceId = null;
    this.resourceType = null;
  }

  public String getPermission() {
    return permission;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }
}
