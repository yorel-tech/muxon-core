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
package com.yorel.muxon.providers;

import com.yorel.muxon.api.model.ProviderType;
import java.util.Map;
import java.util.Optional;

/**
 * Provider-specific context containing all necessary data for VM operations. Each provider type
 * implements this interface to provide its specific context.
 */
public interface ProviderContext {

  /** Get the provider type */
  ProviderType getProviderType();

  /** Get provider connection information (endpoint, credentials) */
  ProviderConnectionInfo getConnectionInfo();

  /** Get placement information specific to this provider */
  Optional<ProviderPlacementInfo> getPlacementInfo();

  /** Get provider-specific metadata */
  Map<String, Object> getMetadata();

  /** Get target resource reference (name + external ID) */
  Optional<Reference> getTargetResource();
}
