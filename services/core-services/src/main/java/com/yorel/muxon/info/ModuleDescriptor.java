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
package com.yorel.muxon.info;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Describes a backend module or integration that contributes core behavior (e.g. a hypervisor,
 * storage backend, or identity provider).
 *
 * <p>Basic operations provided by a module (such as VM CRUD for compute modules) are considered
 * implicit and are intentionally not exposed as capabilities.
 */
@Data
@Builder
public class ModuleDescriptor {

  /** Stable identifier for the module, e.g. "compute.kvm" or "storage.ceph". */
  private String id;

  /** Human-readable name suitable for display in the UI. */
  private String displayName;

  /** High-level category such as "compute", "storage", "network", "identity". */
  private String category;

  /** Indicates whether the module is built into the product or provided by an external plugin. */
  private boolean builtin;

  /**
   * Arbitrary metadata that modules may contribute, such as version, vendor, or configuration
   * hints.
   */
  private Map<String, String> metadata;
}
