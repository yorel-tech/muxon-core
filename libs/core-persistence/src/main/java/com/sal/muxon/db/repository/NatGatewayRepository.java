package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.NatGatewayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NatGatewayRepository extends JpaRepository<NatGatewayEntity, UUID> {

    List<NatGatewayEntity> findByVpcId(UUID vpcId);
}
