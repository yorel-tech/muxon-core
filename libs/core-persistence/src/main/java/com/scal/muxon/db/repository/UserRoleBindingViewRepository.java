package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.UserRoleBindingViewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleBindingViewRepository extends JpaRepository<UserRoleBindingViewEntity, UUID> {

    /**
     * Find user role bindings by external id
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.externalId = :externalId")
    List<UserRoleBindingViewEntity> findByExternalId(@Param("externalId") String externalId);

    /**
     * Find user role bindings by user email
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.email = :email")
    List<UserRoleBindingViewEntity> findByEmail(@Param("email") String email);

    /**
     * Find user role bindings by role name
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.roleName = :roleName")
    List<UserRoleBindingViewEntity> findByRoleName(@Param("roleName") String roleName);

    /**
     * Find user role bindings by scope type and scope id
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType AND u.scopeId = :scopeId")
    List<UserRoleBindingViewEntity> findByScope(@Param("scopeType") String scopeType, @Param("scopeId") UUID scopeId);

    /**
     * Find user role bindings by scope type and scope id with pagination
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType AND u.scopeId = :scopeId")
    Page<UserRoleBindingViewEntity> findByScopeTypeAndScopeId(
            @Param("scopeType") String scopeType,
            @Param("scopeId") UUID scopeId,
            Pageable pageable);

    /**
     * Find user role bindings by scope type, scope id, and username/email with pagination
     */
    @Query("SELECT u FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType AND u.scopeId = :scopeId AND (LOWER(u.username) LIKE LOWER(:query) OR LOWER(u.email) LIKE LOWER(:query))")
    Page<UserRoleBindingViewEntity> findByScopeTypeAndScopeIdAndUsernameOrEmail(
            @Param("scopeType") String scopeType,
            @Param("scopeId") UUID scopeId,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT COUNT(u) FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType")
    long countByScopeType(@Param("scopeType") String scopeType);

    @Query("SELECT COUNT(u) FROM UserRoleBindingViewEntity u WHERE u.scopeType = :scopeType AND u.scopeId = :scopeId")
    long countByScopeTypeAndScopeId(
            @Param("scopeType") String scopeType,
            @Param("scopeId") UUID scopeId);
}
