package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUserEntity, java.util.UUID> {

}
