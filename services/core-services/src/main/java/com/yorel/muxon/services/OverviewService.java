package com.yorel.muxon.services;

import com.yorel.muxon.api.model.SystemOverview;
import com.yorel.muxon.api.model.TenantOverview;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.db.repository.DatacenterRepository;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import com.yorel.muxon.db.repository.TenantRepository;
import com.yorel.muxon.db.repository.UserRoleBindingViewRepository;
import com.yorel.muxon.db.repository.VmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OverviewService {

    @Autowired
    private DatacenterRepository datacenterRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRoleBindingViewRepository userRoleBindingViewRepository;

    @Autowired
    private VmRepository vmRepository;

    @Autowired
    private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

    public SystemOverview getSystemOverview() {
        UUID systemId = UUID.fromString(Constants.SYSTEM_ID);
        long dc = datacenterRepository.count();
        long tenants = tenantRepository.countExcludingId(systemId);
        long tenantUsers = userRoleBindingViewRepository.countByScopeType("TENANT");
        long vms = vmRepository.count();
        return new SystemOverview(dc, tenants, tenantUsers, vms);
    }

    public TenantOverview getTenantOverview(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new EntityNotFoundException("Tenant not found: " + tenantId);
        }
        long grants = tenantDatacenterGrantRepository.countByTenant_Id(tenantId);
        long users = userRoleBindingViewRepository.countByScopeTypeAndScopeId("TENANT", tenantId);
        long vms = vmRepository.countByTenantId(tenantId);
        return new TenantOverview(grants, users, vms);
    }
}
