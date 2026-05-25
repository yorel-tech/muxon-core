package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.DatacenterNetworkCapabilitiesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DatacenterNetworkCapabilitiesRepository extends JpaRepository<DatacenterNetworkCapabilitiesEntity, UUID> {

    Optional<DatacenterNetworkCapabilitiesEntity> findByDatacenterId(UUID datacenterId);
}
