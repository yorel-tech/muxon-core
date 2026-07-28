package com.scal.muxon.services;

import com.scal.muxon.db.model.DatacenterEntity;
import com.scal.muxon.db.model.PublicIpPoolEntity;
import com.scal.muxon.db.repository.DatacenterRepository;
import com.scal.muxon.db.repository.PublicIpPoolRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PublicIpPoolsService {

    @Autowired
    private PublicIpPoolRepository poolRepository;

    @Autowired
    private DatacenterRepository datacenterRepository;

    public List<PublicIpPoolEntity> listByDatacenter(UUID datacenterId) {
        return poolRepository.findByDatacenterId(datacenterId);
    }

    @Transactional
    public PublicIpPoolEntity create(UUID datacenterId, String cidr, String name, String description) {
        DatacenterEntity dc = datacenterRepository.findById(datacenterId)
                .orElseThrow(() -> new EntityNotFoundException("Datacenter not found: " + datacenterId));

        if (poolRepository.existsByDatacenterIdAndCidr(datacenterId, cidr)) {
            throw new IllegalStateException("CONFLICT: Public IP pool with CIDR '" + cidr + "' already exists in this datacenter");
        }

        PublicIpPoolEntity entity = new PublicIpPoolEntity();
        entity.setDatacenter(dc);
        entity.setCidr(cidr);
        entity.setName(name);
        entity.setDescription(description);
        entity.setTotalIps(computeTotalIps(cidr));
        entity.setAllocatedIps(0);
        entity.setStatus("ACTIVE");
        return poolRepository.save(entity);
    }

    public PublicIpPoolEntity getById(UUID poolId) {
        return poolRepository.findById(poolId)
                .orElseThrow(() -> new EntityNotFoundException("Public IP pool not found: " + poolId));
    }

    @Transactional
    public void delete(UUID poolId) {
        if (!poolRepository.existsById(poolId)) {
            throw new EntityNotFoundException("Public IP pool not found: " + poolId);
        }
        poolRepository.deleteById(poolId);
    }

    @Transactional
    public PublicIpPoolEntity incrementAllocated(UUID poolId) {
        PublicIpPoolEntity pool = getById(poolId);
        if (pool.getAllocatedIps() >= pool.getTotalIps()) {
            throw new IllegalStateException("POOL_EXHAUSTED: No available IPs in pool " + poolId);
        }
        pool.setAllocatedIps(pool.getAllocatedIps() + 1);
        return poolRepository.save(pool);
    }

    @Transactional
    public PublicIpPoolEntity decrementAllocated(UUID poolId) {
        PublicIpPoolEntity pool = getById(poolId);
        if (pool.getAllocatedIps() > 0) {
            pool.setAllocatedIps(pool.getAllocatedIps() - 1);
        }
        return poolRepository.save(pool);
    }

    private int computeTotalIps(String cidr) {
        try {
            int prefix = Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
            return (int) Math.pow(2, 32 - prefix) - 2;
        } catch (Exception e) {
            return 0;
        }
    }
}
