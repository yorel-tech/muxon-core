package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.PermissionEntity;
import com.onetattva.infron.db.RoleScopeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID> {

    Optional<PermissionEntity> findByAction(String action);

    Page<PermissionEntity> findByScope(RoleScopeType scope, Pageable pageable);

}
