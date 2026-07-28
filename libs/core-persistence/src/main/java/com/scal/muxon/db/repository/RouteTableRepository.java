package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.RouteTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteTableRepository extends JpaRepository<RouteTableEntity, UUID> {

    List<RouteTableEntity> findByVpcId(UUID vpcId);

    Optional<RouteTableEntity> findByVpcIdAndIsMain(UUID vpcId, boolean isMain);
}
