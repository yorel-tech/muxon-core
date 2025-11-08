package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantUserRepository extends JpaRepository<TenantUser, java.util.UUID> {

}
