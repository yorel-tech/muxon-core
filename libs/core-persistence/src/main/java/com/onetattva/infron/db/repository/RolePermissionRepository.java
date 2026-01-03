package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.role.id IN :roleIds")
    List<RolePermissionEntity> findByRoleIds(@Param("roleIds") List<UUID> roleIds);

    @Query("SELECT rp.permission.id FROM RolePermissionEntity rp WHERE rp.role.id IN :roleIds")
    List<UUID> findPermissionIdsByRoleIds(@Param("roleIds") List<UUID> roleIds);

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.role.name IN :roleNames")
    List<RolePermissionEntity> findByRoleNames(@Param("roleNames") List<String> roleNames);
}
