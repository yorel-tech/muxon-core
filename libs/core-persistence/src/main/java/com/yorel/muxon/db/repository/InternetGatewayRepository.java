package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.InternetGatewayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternetGatewayRepository extends JpaRepository<InternetGatewayEntity, UUID> {

    Optional<InternetGatewayEntity> findByVpcId(UUID vpcId);

    boolean existsByVpcId(UUID vpcId);
}
