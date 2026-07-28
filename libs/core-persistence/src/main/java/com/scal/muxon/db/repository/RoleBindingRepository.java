package com.scal.muxon.db.repository;

import com.scal.muxon.api.enums.RoleBindingSubjectType;
import com.scal.muxon.api.enums.RoleScopeType;
import com.scal.muxon.db.model.RoleBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleBindingRepository extends JpaRepository<RoleBindingEntity, UUID>, JpaSpecificationExecutor<RoleBindingEntity> {

    List<RoleBindingEntity> findByRoleName(String roleName);

    List<RoleBindingEntity> findBySubjectTypeAndSubjectId(RoleBindingSubjectType subjectType, String subjectId);

    List<RoleBindingEntity> findByRoleNameAndScopeTypeAndScopeId(String roleName, RoleScopeType scopeType, UUID scopeId);

}
