package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, java.util.UUID> {

    java.util.Optional<TenantEntity> findByNameIgnoreCase(String name);
}
