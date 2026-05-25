package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.SecurityGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityGroupRepository extends JpaRepository<SecurityGroupEntity, UUID> {

    List<SecurityGroupEntity> findByVpcId(UUID vpcId);

    boolean existsByVpcIdAndName(UUID vpcId, String name);
}
