package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, java.util.UUID> {

}
