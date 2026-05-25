package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.SubnetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubnetRepository extends JpaRepository<SubnetEntity, UUID> {

    List<SubnetEntity> findByVpcId(UUID vpcId);

    List<SubnetEntity> findByVpcIdAndDatacenterId(UUID vpcId, UUID datacenterId);

    long countByVpcId(UUID vpcId);

    /**
     * Check for CIDR overlap within the same VPC (excluding a specific subnet by ID if updating).
     * Uses PostgreSQL inet operators via a native query.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM subnet s WHERE s.vpc_id = :vpcId AND s.id != :excludeId " +
            "AND (inet(s.cidr) >>= inet(:cidr) OR inet(:cidr) >>= inet(s.cidr))",
            nativeQuery = true)
    boolean existsOverlappingCidrInVpc(
            @Param("vpcId") UUID vpcId,
            @Param("cidr") String cidr,
            @Param("excludeId") UUID excludeId);
}
