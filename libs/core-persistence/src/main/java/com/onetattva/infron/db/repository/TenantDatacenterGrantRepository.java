package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantDatacenterGrantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantDatacenterGrantRepository extends JpaRepository<TenantDatacenterGrantEntity, java.util.UUID> {

}
