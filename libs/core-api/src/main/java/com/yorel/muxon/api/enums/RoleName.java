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
package com.yorel.muxon.api.enums;

/**
 * Enum representing predefined role names in the system. These are the default roles used for
 * access control.
 */
public enum RoleName {
  /** System administrator role with full system-level access. */
  SYSTEM_ADMIN("system:admin"),

  /** Tenant administrator role with full tenant-level access. */
  TENANT_ADMIN("tenant:admin"),

  /** Workload operator role with ability to manage workloads. */
  WORKLOAD_OPERATOR("workload:operator"),

  /** Workload user role with read-only access to workloads. */
  WORKLOAD_USER("workload:user");

  private final String value;

  RoleName(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }
}
