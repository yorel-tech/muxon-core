package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.IdentityProviderEntity;
import com.onetattva.infron.db.model.IdentityProviderProtocol;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdentityProviderRepository extends JpaRepository<IdentityProviderEntity, UUID> {

    /**
     * Find identity providers by protocol
     */
    List<IdentityProviderEntity> findByProtocol(IdentityProviderProtocol protocol);

    /**
     * Find enabled identity providers by protocol
     */
    @Query("SELECT i FROM IdentityProviderEntity i WHERE i.protocol = :protocol AND i.enabled = true")
    List<IdentityProviderEntity> findEnabledByProtocol(@Param("protocol") IdentityProviderProtocol protocol);

    /**
     * Find system-level identity provider (is_system = true)
     */
    @Query("SELECT i FROM IdentityProviderEntity i WHERE i.isSystem = true")
    List<IdentityProviderEntity> findSystemProvider();
}
