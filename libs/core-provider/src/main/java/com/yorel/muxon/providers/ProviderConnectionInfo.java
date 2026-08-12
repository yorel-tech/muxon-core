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

import java.util.Map;

/** Connection information for a provider */
public record ProviderConnectionInfo(
    String endpoint, Map<String, String> credentials, Map<String, Object> connectionConfig) {
  public static ProviderConnectionInfoBuilder builder() {
    return new ProviderConnectionInfoBuilder();
  }

  public static class ProviderConnectionInfoBuilder {
    private String endpoint;
    private Map<String, String> credentials;
    private Map<String, Object> connectionConfig;

    public ProviderConnectionInfoBuilder endpoint(String endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public ProviderConnectionInfoBuilder credentials(Map<String, String> credentials) {
      this.credentials = credentials;
      return this;
    }

    public ProviderConnectionInfoBuilder connectionConfig(Map<String, Object> connectionConfig) {
      this.connectionConfig = connectionConfig;
      return this;
    }

    public ProviderConnectionInfo build() {
      return new ProviderConnectionInfo(endpoint, credentials, connectionConfig);
    }
  }
}
