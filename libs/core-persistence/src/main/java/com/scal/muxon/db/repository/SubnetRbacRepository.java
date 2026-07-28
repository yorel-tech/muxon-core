package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.SubnetRbacEntity;
import com.scal.muxon.db.model.SubnetRbacEntity.SubnetPrincipalType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubnetRbacRepository extends JpaRepository<SubnetRbacEntity, UUID> {

    List<SubnetRbacEntity> findBySubnetId(UUID subnetId);

    Optional<SubnetRbacEntity> findBySubnetIdAndPrincipalTypeAndPrincipalId(
            UUID subnetId, SubnetPrincipalType principalType, UUID principalId);

    @Query("SELECT COUNT(r) > 0 FROM SubnetRbacEntity r WHERE r.subnet.id = :subnetId " +
            "AND r.principalId = :principalId AND :permission MEMBER OF r.permissions")
    boolean hasPermission(
            @Param("subnetId") UUID subnetId,
            @Param("principalId") UUID principalId,
            @Param("permission") String permission);

    @Query("SELECT DISTINCT s.subnet.id FROM SubnetRbacEntity s WHERE s.principalId = :principalId " +
            "AND s.subnet.vpc.id = :vpcId " +
            "AND (s.permissions IS NOT EMPTY)")
    List<UUID> findAuthorizedSubnetIds(
            @Param("principalId") UUID principalId,
            @Param("vpcId") UUID vpcId);
}
