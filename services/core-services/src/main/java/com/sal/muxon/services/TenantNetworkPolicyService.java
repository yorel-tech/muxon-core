package com.sal.muxon.services;

import com.sal.muxon.db.model.TenantEntity;
import com.sal.muxon.db.model.TenantNetworkPolicyEntity;
import com.sal.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import com.sal.muxon.db.repository.TenantNetworkPolicyRepository;
import com.sal.muxon.db.repository.DatacenterNetworkCapabilitiesRepository;
import com.sal.muxon.db.repository.TenantRepository;
import com.sal.muxon.db.repository.DatacenterRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantNetworkPolicyService {

    @Autowired
    private TenantNetworkPolicyRepository policyRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private DatacenterNetworkCapabilitiesRepository capabilitiesRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    @Transactional
    public TenantNetworkPolicyEntity getOrCreateDefaultPolicy(TenantEntity tenant) {
        return policyRepository.findByTenantId(tenant.getId())
                .orElseGet(() -> createDefaultPolicy(tenant));
    }

    @Transactional
    public TenantNetworkPolicyEntity createDefaultPolicy(TenantEntity tenant) {
        TenantNetworkPolicyEntity policy = new TenantNetworkPolicyEntity();
        policy.setTenant(tenant);
        policy.setMaxVpcs(10);
        policy.setMaxPublicIps(5);
        policy.setMaxSubnetsPerVpc(10);
        policy.setVpnAllowed(false);
        policy.setPeeringAllowed(false);
        policy.setHaNetworkingAllowed(false);
        return policyRepository.save(policy);
    }

    public TenantNetworkPolicyEntity getPolicy(UUID tenantId) {
        return policyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Network policy not found for tenant: " + tenantId));
    }

    @Transactional
    public TenantNetworkPolicyEntity updatePolicy(UUID tenantId,
                                                   Integer maxVpcs,
                                                   Integer maxPublicIps,
                                                   Integer maxSubnetsPerVpc,
                                                   Boolean vpnAllowed,
                                                   Boolean peeringAllowed,
                                                   Boolean haNetworkingAllowed,
                                                   Integer bandwidthLimitMbps) {
        TenantNetworkPolicyEntity policy = getPolicy(tenantId);
        if (maxVpcs != null) policy.setMaxVpcs(maxVpcs);
        if (maxPublicIps != null) policy.setMaxPublicIps(maxPublicIps);
        if (maxSubnetsPerVpc != null) policy.setMaxSubnetsPerVpc(maxSubnetsPerVpc);
        if (vpnAllowed != null) policy.setVpnAllowed(vpnAllowed);
        if (peeringAllowed != null) policy.setPeeringAllowed(peeringAllowed);
        if (haNetworkingAllowed != null) policy.setHaNetworkingAllowed(haNetworkingAllowed);
        if (bandwidthLimitMbps != null) policy.setBandwidthLimitMbps(bandwidthLimitMbps);
        return policyRepository.save(policy);
    }

    public EffectiveCapabilities resolveEffectiveCapabilities(UUID tenantId, UUID datacenterId) {
        TenantNetworkPolicyEntity policy = getPolicy(tenantId);
        DatacenterNetworkCapabilitiesEntity caps = capabilitiesRepository.findByDatacenterId(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Network capabilities not found for datacenter: " + datacenterId));

        return new EffectiveCapabilities(
                caps.isPublicIpSupported(),
                policy.isVpnAllowed() && caps.isVpnSupported(),
                caps.isBgpSupported(),
                policy.isHaNetworkingAllowed() && caps.isHaGatewaySupported(),
                caps.isVxlanSupported(),
                caps.isMultiRegionSupported(),
                caps.isL7LbSupported(),
                caps.isIpv6Supported(),
                caps.isDualStackSupported()
        );
    }

    public record EffectiveCapabilities(
            boolean publicIpAvailable,
            boolean vpnAvailable,
            boolean bgpAvailable,
            boolean haGatewayAvailable,
            boolean vxlanAvailable,
            boolean multiRegionAvailable,
            boolean l7LbAvailable,
            boolean ipv6Available,
            boolean dualStackAvailable
    ) {}
}
