package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.PublicIpPoolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PublicIpPoolRepository extends JpaRepository<PublicIpPoolEntity, UUID> {

    List<PublicIpPoolEntity> findByDatacenterId(UUID datacenterId);

    @Query("SELECT p FROM PublicIpPoolEntity p WHERE p.datacenter.id = :datacenterId AND p.status = 'ACTIVE' AND p.allocatedIps < p.totalIps")
    List<PublicIpPoolEntity> findAvailableByDatacenterId(@Param("datacenterId") UUID datacenterId);

    boolean existsByDatacenterIdAndCidr(UUID datacenterId, String cidr);
}
