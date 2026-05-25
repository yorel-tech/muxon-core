package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.IpamPrefixDelegationEntity;
import com.sal.muxon.db.model.IpamPrefixDelegationEntity.IpamDelegationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IpamPrefixDelegationRepository extends JpaRepository<IpamPrefixDelegationEntity, UUID> {

    List<IpamPrefixDelegationEntity> findBySubnetId(UUID subnetId);

    List<IpamPrefixDelegationEntity> findBySubnetIdAndStatus(UUID subnetId, IpamDelegationStatus status);

    List<IpamPrefixDelegationEntity> findByStatus(IpamDelegationStatus status);

    /**
     * Check for CIDR overlap in active delegations within the same subnet.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM ipam_prefix_delegation d " +
            "WHERE d.subnet_id = :subnetId AND d.id != :excludeId AND d.status IN ('PENDING', 'ACTIVE') " +
            "AND (inet(d.delegated_cidr) >>= inet(:cidr) OR inet(:cidr) >>= inet(d.delegated_cidr))",
            nativeQuery = true)
    boolean existsOverlappingDelegation(
            @Param("subnetId") UUID subnetId,
            @Param("cidr") String cidr,
            @Param("excludeId") UUID excludeId);
}
