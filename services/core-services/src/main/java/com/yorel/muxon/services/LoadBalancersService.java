package com.yorel.muxon.services;

import com.yorel.muxon.db.model.FloatingIpEntity;
import com.yorel.muxon.db.model.LoadBalancerEntity;
import com.yorel.muxon.db.model.LoadBalancerEntity.LbScheme;
import com.yorel.muxon.db.model.LoadBalancerListenerEntity;
import com.yorel.muxon.db.model.LoadBalancerTargetEntity;
import com.yorel.muxon.db.model.SubnetEntity;
import com.yorel.muxon.db.model.VpcEntity;
import com.yorel.muxon.db.repository.FloatingIpRepository;
import com.yorel.muxon.db.repository.LoadBalancerRepository;
import com.yorel.muxon.db.repository.SubnetRepository;
import com.yorel.muxon.db.repository.VpcRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class LoadBalancersService {

    @Autowired
    private LoadBalancerRepository lbRepository;

    @Autowired
    private VpcRepository vpcRepository;

    @Autowired
    private SubnetRepository subnetRepository;

    @Autowired
    private FloatingIpRepository floatingIpRepository;

    // Listener and target repositories would normally be injected here
    // Using entity manager or separate repositories; simplified for brevity

    public List<LoadBalancerEntity> listByVpc(UUID vpcId) {
        return lbRepository.findByVpcId(vpcId);
    }

    @Transactional
    public LoadBalancerEntity create(UUID vpcId, String name, UUID subnetId,
                                      LbScheme scheme, UUID floatingIpId) {
        VpcEntity vpc = vpcRepository.findById(vpcId)
                .orElseThrow(() -> new EntityNotFoundException("VPC not found: " + vpcId));
        SubnetEntity subnet = subnetRepository.findById(subnetId)
                .orElseThrow(() -> new EntityNotFoundException("Subnet not found: " + subnetId));

        if (scheme == LbScheme.INTERNET_FACING && floatingIpId == null) {
            throw new IllegalArgumentException("UNPROCESSABLE: INTERNET_FACING load balancer requires a floatingIpId");
        }

        LoadBalancerEntity entity = new LoadBalancerEntity();
        entity.setVpc(vpc);
        entity.setSubnet(subnet);
        entity.setName(name);
        entity.setScheme(scheme);
        entity.setStatus("ACTIVE");

        if (floatingIpId != null) {
            FloatingIpEntity fip = floatingIpRepository.findById(floatingIpId)
                    .orElseThrow(() -> new EntityNotFoundException("Floating IP not found: " + floatingIpId));
            entity.setFloatingIp(fip);
        }

        return lbRepository.save(entity);
    }

    public LoadBalancerEntity getById(UUID lbId) {
        return lbRepository.findById(lbId)
                .orElseThrow(() -> new EntityNotFoundException("Load balancer not found: " + lbId));
    }

    @Transactional
    public void delete(UUID lbId) {
        if (!lbRepository.existsById(lbId)) {
            throw new EntityNotFoundException("Load balancer not found: " + lbId);
        }
        lbRepository.deleteById(lbId);
    }
}
