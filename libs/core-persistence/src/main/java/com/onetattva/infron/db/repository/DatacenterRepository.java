package com.onetattva.infron.db.repository;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.db.model.DatacenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatacenterRepository extends JpaRepository<DatacenterEntity, java.util.UUID> {

    /**
     * Find a datacenter by its node cluster ID.
     */
    Optional<DatacenterEntity> findByNodeClusterId(UUID nodeClusterId);

    /**
     * Check if a datacenter exists with the given name.
     */
    boolean existsByName(String name);

    /**
     * Find datacenters filtered by provider type.
     * Provider type is derived from the node cluster's provider.
     */
    @Query("SELECT d FROM DatacenterEntity d JOIN d.nodeCluster nc JOIN nc.provider p WHERE p.type = :providerType")
    java.util.List<DatacenterEntity> findByProviderType(@Param("providerType") ProviderType providerType);
}
