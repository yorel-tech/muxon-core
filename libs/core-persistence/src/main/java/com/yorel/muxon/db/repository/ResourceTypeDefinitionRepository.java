package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.ResourceTypeStatus;
import com.yorel.muxon.db.model.ResourceTypeDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResourceTypeDefinitionRepository extends JpaRepository<ResourceTypeDefinitionEntity, UUID> {

    Optional<ResourceTypeDefinitionEntity> findByKind(String kind);

    List<ResourceTypeDefinitionEntity> findByPluginId(UUID pluginId);

    List<ResourceTypeDefinitionEntity> findByStatus(ResourceTypeStatus status);
}
