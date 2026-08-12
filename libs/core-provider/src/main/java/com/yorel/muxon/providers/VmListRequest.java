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

import java.util.List;
import java.util.UUID;

/** Request for VM list */
public record VmListRequest(
    UUID tenantDatacenterGrantId, String statusFilter, List<String> tags, Integer limit) {
  public static VmListRequestBuilder builder() {
    return new VmListRequestBuilder();
  }

  public static class VmListRequestBuilder {
    private UUID tenantDatacenterGrantId;
    private String statusFilter;
    private List<String> tags;
    private Integer limit;

    public VmListRequestBuilder tenantDatacenterGrantId(UUID tenantDatacenterGrantId) {
      this.tenantDatacenterGrantId = tenantDatacenterGrantId;
      return this;
    }

    public VmListRequestBuilder statusFilter(String statusFilter) {
      this.statusFilter = statusFilter;
      return this;
    }

    public VmListRequestBuilder tags(List<String> tags) {
      this.tags = tags;
      return this;
    }

    public VmListRequestBuilder limit(Integer limit) {
      this.limit = limit;
      return this;
    }

    public VmListRequest build() {
      return new VmListRequest(tenantDatacenterGrantId, statusFilter, tags, limit);
    }
  }
}
