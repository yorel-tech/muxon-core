package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.TenantNetworkPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantNetworkPolicyRepository extends JpaRepository<TenantNetworkPolicyEntity, UUID> {

    Optional<TenantNetworkPolicyEntity> findByTenantId(UUID tenantId);
}
