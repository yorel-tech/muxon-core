package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<TenantEntity, java.util.UUID> {

    java.util.Optional<TenantEntity> findByNameIgnoreCase(String name);

    @Query("SELECT COUNT(t) FROM TenantEntity t WHERE t.id <> :systemTenantId")
    long countExcludingId(@Param("systemTenantId") UUID systemTenantId);
}
