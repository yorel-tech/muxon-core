package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.UserRoleBindingViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleBindingViewRepository extends JpaRepository<UserRoleBindingViewEntity, UUID> {

    /**
     * Find user role bindings by external id
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.externalId = :externalId")
    List<UserRoleBindingViewEntity> findByExternalId(String externalId);

    /**
     * Find user role bindings by user email
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.email = :email")
    List<UserRoleBindingViewEntity> findByEmail(String email);

    /**
     * Find user role bindings by role name
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.roleName = :roleName")
    List<UserRoleBindingViewEntity> findByRoleName(String roleName);

    /**
     * Find user role bindings by scope type and scope id
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType AND u.scopeId = :scopeId")
    List<UserRoleBindingViewEntity> findByScope(String scopeType, UUID scopeId);
}
