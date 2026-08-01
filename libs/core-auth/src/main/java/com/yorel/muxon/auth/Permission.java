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
package com.yorel.muxon.auth;

import com.yorel.muxon.common.UuidUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public enum Permission {
  SYSTEM_SETTINGS("system:settings", "Update system settings", Scope.SYSTEM),

  SYSTEM_USER_READ("system:user:read", "Read system user", Scope.SYSTEM),
  SYSTEM_USER_EDIT("system:user:edit", "Edit system user", Scope.SYSTEM),
  SYSTEM_USER_MANAGE("system:user:manage", "Manage system user", Scope.SYSTEM),

  PROVIDER_READ("provider:read", "Read provider", Scope.SYSTEM),
  PROVIDER_EDIT("provider:edit", "Edit provider", Scope.SYSTEM),
  PROVIDER_MANAGE("provider:manage", "Manage provider", Scope.SYSTEM),

  NODE_READ("node:read", "Read hypervisor nodes", Scope.SYSTEM),
  NODE_EDIT("node:edit", "Edit hypervisor nodes", Scope.SYSTEM),
  NODE_MANAGE("node:manage", "Manage hypervisor nodes", Scope.SYSTEM),

  NODE_CLUSTER_READ("node_cluster:read", "Read node clusters", Scope.SYSTEM),
  NODE_CLUSTER_EDIT("node_cluster:edit", "Edit node clusters", Scope.SYSTEM),
  NODE_CLUSTER_MANAGE("node_cluster:manage", "Manage node clusters", Scope.SYSTEM),

  STORAGE_CLASS_READ("storage_class:read", "Read storage classes", Scope.SYSTEM),
  STORAGE_CLASS_EDIT("storage_class:edit", "Edit storage classes", Scope.SYSTEM),
  STORAGE_CLASS_MANAGE("storage_class:manage", "Manage storage classes", Scope.SYSTEM),

  PROVIDER_STORAGE_READ("provider_storage:read", "Read provider storage inventory", Scope.SYSTEM),
  PROVIDER_STORAGE_EDIT(
      "provider_storage:edit", "Sync or modify provider storage mappings", Scope.SYSTEM),

  STORAGE_OVERRIDE_READ("storage_override:read", "Read storage class overrides", Scope.SYSTEM),
  STORAGE_OVERRIDE_EDIT("storage_override:edit", "Edit storage class overrides", Scope.SYSTEM),
  STORAGE_OVERRIDE_MANAGE(
      "storage_override:manage", "Manage storage class overrides", Scope.SYSTEM),

  ROLE_READ("role:read", "Read platform roles", Scope.SYSTEM),
  ROLE_EDIT("role:edit", "Edit platform roles", Scope.SYSTEM),
  ROLE_MANAGE("role:manage", "Manage platform roles", Scope.SYSTEM),

  PERMISSION_READ("permission:read", "List permission catalog", Scope.SYSTEM),

  USER_READ("user:read", "Read user", Scope.TENANT),
  USER_EDIT("user:edit", "Edit user", Scope.TENANT),
  USER_MANAGE("user:manage", "Manage user", Scope.TENANT),

  TENANT_READ("tenant:read", "Read tenant", Scope.SYSTEM),
  TENANT_READ_SETTINGS("tenant:read-settings", "Read tenant settings", Scope.SYSTEM),
  TENANT_EDIT("tenant:edit", "Edit tenant", Scope.SYSTEM),
  TENANT_MANAGE("tenant:manage", "Manage tenant", Scope.SYSTEM),

  DATACENTER_READ("datacenter:read", "Read datacenter", Scope.SYSTEM),
  DATACENTER_EDIT("datacenter:edit", "Edit datacenter", Scope.SYSTEM),
  DATACENTER_MANAGE("datacenter:manage", "Manage datacenter", Scope.SYSTEM),

  TENANT_DATACENTER_READ("tenant:datacenter:read", "Read tenant datacenter grants", Scope.TENANT),

  ROLE_BINDING_READ("role_binding:read", "Read role bindings", Scope.SYSTEM),
  ROLE_BINDING_EDIT("role_binding:edit", "Edit role bindings", Scope.SYSTEM),
  ROLE_BINDING_MANAGE("role_binding:manage", "Manage role bindings", Scope.SYSTEM),

  TENANT_SETTINGS("tenant:settings", "Update tenant settings", Scope.TENANT),

  TENANT_ROLE_READ("tenant_role:read", "Read tenant custom roles", Scope.TENANT),
  TENANT_ROLE_EDIT("tenant_role:edit", "Edit tenant custom roles", Scope.TENANT),
  TENANT_ROLE_MANAGE("tenant_role:manage", "Manage tenant custom roles", Scope.TENANT),

  VM_CREATE("vm:create", "Create VM", Scope.TENANT),
  VM_READ("vm:read", "Read vm", Scope.TENANT),
  VM_EDIT("vm:edit", "Edit vm", Scope.TENANT),
  VM_MANAGE("vm:manage", "Manage vm", Scope.TENANT),
  VM_CONSOLE("vm:console", "View vm console", Scope.TENANT),
  VM_DELETE("vm:delete", "Delete vm", Scope.TENANT),

  COMPUTE_PROFILE_CREATE("compute_profile:create", "Create compute profile", Scope.TENANT),
  COMPUTE_PROFILE_READ("compute_profile:read", "Read compute profile", Scope.TENANT),
  COMPUTE_PROFILE_EDIT("compute_profile:edit", "Edit compute profile", Scope.TENANT),
  COMPUTE_PROFILE_MANAGE("compute_profile:manage", "Manage compute profile", Scope.TENANT),

  CONTENT_LIBRARY_READ("content_library:read", "Read content library", Scope.TENANT),
  CONTENT_LIBRARY_WRITE("content_library:write", "Write to content library", Scope.TENANT),
  CONTENT_LIBRARY_PUBLISH_TEMPLATE(
      "content_library:publish_template",
      "Publish VM as template to content library",
      Scope.TENANT),

  STACK_READ("stack:read", "Read stacks", Scope.TENANT),
  STACK_EDIT("stack:edit", "Edit stacks", Scope.TENANT),
  STACK_MANAGE("stack:manage", "Manage stacks (create/delete)", Scope.TENANT),

  VPC_READ("vpc:read", "Read VPCs and subnets", Scope.TENANT),
  VPC_EDIT("vpc:edit", "Edit VPCs and subnets", Scope.TENANT),
  VPC_MANAGE("vpc:manage", "Manage VPCs (create/delete)", Scope.TENANT),

  SUBNET_RBAC_MANAGE("subnet_rbac:manage", "Manage subnet access bindings", Scope.TENANT),

  NETWORK_POLICY_READ("network_policy:read", "Read tenant network policy", Scope.SYSTEM),
  NETWORK_POLICY_MANAGE("network_policy:manage", "Manage tenant network policy", Scope.SYSTEM),

  FABRIC_NETWORK_MANAGE("fabric_network:manage", "Manage fabric networks", Scope.SYSTEM),
  PUBLIC_IP_POOL_MANAGE("public_ip_pool:manage", "Manage public IP pools", Scope.SYSTEM),
  NETWORK_EDGE_NODE_MANAGE("network_edge_node:manage", "Manage network edge nodes", Scope.SYSTEM),
  DATACENTER_NETWORK_CAPABILITIES_MANAGE(
      "datacenter_network_capabilities:manage",
      "Manage datacenter network capabilities",
      Scope.SYSTEM),

  PLUGIN_READ("plugin:read", "Read plugin registry", Scope.SYSTEM),
  PLUGIN_MANAGE("plugin:manage", "Register, activate, and manage plugins", Scope.SYSTEM);

  private static final UUID NAMESPACE = UUID.fromString("696e6672-6f6e-636f-7265-111111111111");
  private static final Map<UUID, Permission> PERMISSION_BY_ID = new HashMap<>();

  private final String action;
  private final String description;
  private final Scope scope;
  private final UUID id;

  static {
    for (Permission permission : values()) {
      PERMISSION_BY_ID.put(permission.id, permission);
    }
  }

  Permission(String action, String description, Scope scope) {
    this.action = action;
    this.description = description;
    this.scope = scope;
    this.id = generateDeterministicId(action);
  }

  public String getAction() {
    return action;
  }

  public String getDescription() {
    return description;
  }

  public Scope getScope() {
    return scope;
  }

  public UUID getId() {
    return id;
  }

  public static Permission fromAction(String action) {
    for (Permission permission : values()) {
      if (permission.action.equals(action)) {
        return permission;
      }
    }
    return null;
  }

  public static Permission fromId(UUID id) {
    return PERMISSION_BY_ID.get(id);
  }

  private static UUID generateDeterministicId(String action) {
    return UuidUtils.generateUuid5(NAMESPACE, action);
  }

  public enum Scope {
    SYSTEM,
    TENANT,
    TENANT_GLOBAL
  }
}
