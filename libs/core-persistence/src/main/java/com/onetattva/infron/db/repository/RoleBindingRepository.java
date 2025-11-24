package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.RoleBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleBindingRepository extends JpaRepository<RoleBindingEntity, UUID>, JpaSpecificationExecutor<RoleBindingEntity> {

    List<RoleBindingEntity> findByRole_Id(UUID roleId);

    List<RoleBindingEntity> findBySubjectTypeAndSubjectId(String subjectType, String subjectId);

    List<RoleBindingEntity> findByRole_IdAndScopeTypeAndScopeId(UUID roleId, String scopeType, UUID scopeId);

}
