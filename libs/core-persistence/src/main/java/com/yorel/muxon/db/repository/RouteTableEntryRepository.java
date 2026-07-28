package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.RouteTableEntryEntity;
import com.yorel.muxon.db.model.RouteTableEntryEntity.RouteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RouteTableEntryRepository extends JpaRepository<RouteTableEntryEntity, UUID> {

    List<RouteTableEntryEntity> findByRouteTableId(UUID routeTableId);

    Optional<RouteTableEntryEntity> findByRouteTableIdAndTargetTypeAndTargetId(
            UUID routeTableId, RouteTargetType targetType, UUID targetId);

    void deleteByRouteTableIdAndTargetTypeAndTargetId(
            UUID routeTableId, RouteTargetType targetType, UUID targetId);
}
