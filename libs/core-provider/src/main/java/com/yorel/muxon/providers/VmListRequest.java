package com.yorel.muxon.providers;

import java.util.List;
import java.util.UUID;

/**
 * Request for VM list
 */
public record VmListRequest(
        UUID tenantDatacenterGrantId,
        String statusFilter,
        List<String> tags,
        Integer limit
) {
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
