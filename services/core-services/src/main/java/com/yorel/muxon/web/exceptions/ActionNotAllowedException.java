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

/**
 * Exception thrown when an action is not allowed due to business rules or resource state. This is
 * different from PermissionDeniedException - the user may have permission but the action is not
 * allowed due to current resource state or dependencies.
 */
public class ActionNotAllowedException extends RuntimeException {

  private final String action;
  private final UUID resourceId;
  private final String resourceType;
  private final String reason;

  public ActionNotAllowedException(
      String action, UUID resourceId, String resourceType, String reason) {
    super(
        String.format(
            "Action '%s' not allowed on %s with id %s: %s",
            action, resourceType, resourceId, reason));
    this.action = action;
    this.resourceId = resourceId;
    this.resourceType = resourceType;
    this.reason = reason;
  }

  public ActionNotAllowedException(String action, String resourceType, String reason) {
    super(String.format("Action '%s' not allowed on %s: %s", action, resourceType, reason));
    this.action = action;
    this.resourceId = null;
    this.resourceType = resourceType;
    this.reason = reason;
  }

  public ActionNotAllowedException(String message) {
    super(message);
    this.action = null;
    this.resourceId = null;
    this.resourceType = null;
    this.reason = null;
  }

  public String getAction() {
    return action;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getResourceType() {
    return resourceType;
  }

  public String getReason() {
    return reason;
  }
}
