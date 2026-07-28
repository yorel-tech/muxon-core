package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.SecurityGroupRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SecurityGroupRuleRepository extends JpaRepository<SecurityGroupRuleEntity, UUID> {

    List<SecurityGroupRuleEntity> findBySecurityGroupId(UUID securityGroupId);
}
