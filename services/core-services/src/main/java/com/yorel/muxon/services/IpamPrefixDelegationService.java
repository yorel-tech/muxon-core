package com.yorel.muxon.services;

import com.yorel.muxon.db.model.IpamPrefixDelegationEntity;
import com.yorel.muxon.db.model.IpamPrefixDelegationEntity.IpamDelegationStatus;
import com.yorel.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import com.yorel.muxon.db.model.StackEntity;
import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.repository.IpamPrefixDelegationRepository;
import com.yorel.muxon.db.repository.StackRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class IpamPrefixDelegationService {

    @Autowired
    private IpamPrefixDelegationRepository delegationRepository;

    @Autowired
    private SubnetRepository subnetRepository;

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private RouteTablesService routeTablesService;

    public List<IpamPrefixDelegationEntity> listBySubnet(UUID subnetId) {
        return delegationRepository.findBySubnetId(subnetId);
    }

    @Transactional
    public IpamPrefixDelegationEntity create(UUID subnetId, UUID stackId, UUID nodeVmId, String delegatedCidr) {
        SubnetEntity subnet = subnetRepository.findById(subnetId)
                .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

        StackEntity stack = stackRepository.findById(stackId)
                .orElseThrow(() -> new EntityNotFoundException("Stack not found: " + stackId));

        // Validate delegatedCidr is within subnet CIDR
        if (!isCidrContainedIn(delegatedCidr, subnet.getCidr())) {
            throw new IllegalArgumentException("INVALID_CIDR: Delegated CIDR " + delegatedCidr
                    + " must be within subnet CIDR " + subnet.getCidr());
        }

        // Check for overlap with existing active delegations
        UUID nullUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
        if (delegationRepository.existsOverlappingDelegation(subnetId, delegatedCidr, nullUUID)) {
            throw new IllegalStateException("CONFLICT: Delegated CIDR " + delegatedCidr + " overlaps with existing delegation");
        }

        IpamPrefixDelegationEntity entity = new IpamPrefixDelegationEntity();
        entity.setSubnet(subnet);
        entity.setStack(stack);
        entity.setNodeVmId(nodeVmId);
        entity.setDelegatedCidr(delegatedCidr);
        entity.setStatus(IpamDelegationStatus.PENDING);
        IpamPrefixDelegationEntity saved = delegationRepository.save(entity);

        // Attempt to auto-create route table entry
        tryCreateRouteEntry(saved, subnet, nodeVmId, delegatedCidr);

        return delegationRepository.save(saved);
    }

    @Transactional
    public void release(UUID delegationId) {
        IpamPrefixDelegationEntity entity = delegationRepository.findById(delegationId)
                .orElseThrow(() -> new EntityNotFoundException("Delegation not found: " + delegationId));

        // Remove route table entry
        if (entity.getSubnet().getRouteTable() != null) {
            try {
                routeTablesService.listEntries(entity.getSubnet().getRouteTable().getId())
                        .stream()
                        .filter(e -> entity.getNodeVmId().equals(e.getTargetId())
                                && entity.getDelegatedCidr().equals(e.getDestinationCidr()))
                        .findFirst()
                        .ifPresent(e -> routeTablesService.deleteEntry(e.getId()));
            } catch (Exception ignored) {
            }
        }

        entity.setStatus(IpamDelegationStatus.RELEASED);
        delegationRepository.save(entity);
    }

    public List<IpamPrefixDelegationEntity> findRouteErrorDelegations() {
        return delegationRepository.findByStatus(IpamDelegationStatus.ROUTE_ERROR);
    }

    @Transactional
    public void reconcileRouteErrors() {
        findRouteErrorDelegations().forEach(delegation -> {
            tryCreateRouteEntry(delegation, delegation.getSubnet(),
                    delegation.getNodeVmId(), delegation.getDelegatedCidr());
            delegationRepository.save(delegation);
        });
    }

    private void tryCreateRouteEntry(IpamPrefixDelegationEntity delegation, SubnetEntity subnet,
                                      UUID nodeVmId, String delegatedCidr) {
        try {
            if (subnet.getRouteTable() != null) {
                routeTablesService.addEntry(
                        subnet.getRouteTable().getId(),
                        delegatedCidr,
                        RouteTargetType.INSTANCE,
                        nodeVmId);
                delegation.setStatus(IpamDelegationStatus.ACTIVE);
            } else {
                delegation.setStatus(IpamDelegationStatus.ACTIVE);
            }
        } catch (Exception e) {
            delegation.setStatus(IpamDelegationStatus.ROUTE_ERROR);
        }
    }

    private boolean isCidrContainedIn(String cidr, String parentCidr) {
        try {
            String[] cidrParts = cidr.split("/");
            String[] parentParts = parentCidr.split("/");
            int cidrPrefix = Integer.parseInt(cidrParts[1]);
            int parentPrefix = Integer.parseInt(parentParts[1]);
            if (cidrPrefix < parentPrefix) return false;
            long cidrAddr = ipToLong(cidrParts[0]);
            long parentAddr = ipToLong(parentParts[0]);
            long mask = prefixToMask(parentPrefix);
            return (cidrAddr & mask) == (parentAddr & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        long result = 0;
        for (String part : parts) result = result * 256 + Integer.parseInt(part.trim());
        return result;
    }

    private long prefixToMask(int prefix) {
        return prefix == 0 ? 0 : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
    }
}
