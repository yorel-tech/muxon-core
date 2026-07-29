package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.LoadBalancerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoadBalancerRepository extends JpaRepository<LoadBalancerEntity, UUID> {

    List<LoadBalancerEntity> findByVpcId(UUID vpcId);
}
