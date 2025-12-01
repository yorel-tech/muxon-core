package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.RoleEntity;
import com.onetattva.infron.db.RoleScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    Page<RoleEntity> findByScopeType(RoleScopeType scopeType, Pageable pageable);

    Page<RoleEntity> findByScopeTypeAndScopeId(RoleScopeType scopeType, UUID scopeId, Pageable pageable);

    Page<RoleEntity> findByScopeTypeIn(Set<RoleScopeType> scopeTypes, Pageable pageable);

    RoleEntity findByNameAndScopeId(String name, UUID scopeId);

    RoleEntity findByNameAndScopeIdIsNull(String name);

}
