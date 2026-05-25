package com.sal.muxon.services;

import com.sal.muxon.db.model.RouteTableEntity;
import com.sal.muxon.db.model.RouteTableEntryEntity;
import com.sal.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import com.sal.muxon.db.model.TenantEntity;
import com.sal.muxon.db.model.VpcEntity;
import com.sal.muxon.db.model.VpcEntity.VpcStatus;
import com.sal.muxon.db.repository.RouteTableEntryRepository;
import com.sal.muxon.db.repository.RouteTableRepository;
import com.sal.muxon.db.repository.SubnetRepository;
import com.sal.muxon.db.repository.TenantNetworkPolicyRepository;
import com.sal.muxon.db.repository.TenantRepository;
import com.sal.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VpcService {

    private static final String[] PRIVATE_CIDRS = {"10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.", "192.168."};

    @Autowired
    private VpcRepository vpcRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantNetworkPolicyRepository policyRepository;

    @Autowired
    private RouteTableRepository routeTableRepository;

    @Autowired
    private RouteTableEntryRepository routeTableEntryRepository;

    @Autowired
    private SubnetRepository subnetRepository;

    public List<VpcEntity> listByTenant(UUID tenantId) {
        return vpcRepository.findByTenantId(tenantId);
    }

    @Transactional
    public VpcEntity create(UUID tenantId, String name, String cidr, String description,
                             Map<String, String> metadata) {
        if (cidr != null && !isPrivateCidr(cidr)) {
            throw new IllegalArgumentException("INVALID_CIDR: VPC CIDR must be a private (RFC 1918) range");
        }

        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));

        // Quota check
        policyRepository.findByTenantId(tenantId).ifPresent(policy -> {
            long count = vpcRepository.countByTenantId(tenantId);
            if (count >= policy.getMaxVpcs()) {
                throw new IllegalStateException("QUOTA_EXCEEDED: Tenant has reached max_vpcs limit of " + policy.getMaxVpcs());
            }
        });

        if (vpcRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new IllegalStateException("CONFLICT: VPC with name '" + name + "' already exists for this tenant");
        }

        VpcEntity vpc = new VpcEntity();
        vpc.setTenant(tenant);
        vpc.setName(name);
        vpc.setCidr(cidr);
        vpc.setDescription(description);
        vpc.setMetadata(metadata);
        vpc.setStatus(VpcStatus.ACTIVE);
        VpcEntity saved = vpcRepository.save(vpc);

        // Auto-create main route table with LOCAL entry for VPC CIDR
        RouteTableEntity mainRt = new RouteTableEntity();
        mainRt.setVpc(saved);
        mainRt.setName("main");
        mainRt.setMain(true);
        RouteTableEntity savedRt = routeTableRepository.save(mainRt);

        RouteTableEntryEntity localEntry = new RouteTableEntryEntity();
        localEntry.setRouteTable(savedRt);
        localEntry.setDestinationCidr(cidr);
        localEntry.setTargetType(RouteTargetType.LOCAL);
        routeTableEntryRepository.save(localEntry);

        return saved;
    }

    public VpcEntity getById(UUID vpcId) {
        return vpcRepository.findById(vpcId)
                .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));
    }

    public VpcEntity getByIdAndTenant(UUID vpcId, UUID tenantId) {
        VpcEntity vpc = getById(vpcId);
        if (!vpc.getTenant().getId().equals(tenantId)) {
            throw new EntityNotFoundException("VPC not found: " + vpcId);
        }
        return vpc;
    }

    @Transactional
    public VpcEntity update(UUID vpcId, String name, String description) {
        VpcEntity entity = getById(vpcId);
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        return vpcRepository.save(entity);
    }

    @Transactional
    public void delete(UUID vpcId) {
        VpcEntity entity = getById(vpcId);
        long activeSubnets = subnetRepository.countByVpcId(vpcId);
        if (activeSubnets > 0) {
            throw new IllegalStateException("CONFLICT: VPC has " + activeSubnets + " active subnet(s); delete them first");
        }
        entity.setStatus(VpcStatus.DELETING);
        vpcRepository.save(entity);
    }

    private boolean isPrivateCidr(String cidr) {
        if (cidr == null) return false;
        for (String prefix : PRIVATE_CIDRS) {
            if (cidr.startsWith(prefix)) return true;
        }
        return false;
    }
}
