package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantUserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUserRoleRepository extends JpaRepository<TenantUserRoleEntity, java.util.UUID> {

}
