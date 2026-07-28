package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.CatalogContributionStatus;
import com.yorel.muxon.db.model.PluginCatalogContributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PluginCatalogContributionRepository extends JpaRepository<PluginCatalogContributionEntity, UUID> {

    List<PluginCatalogContributionEntity> findByStatus(CatalogContributionStatus status);

    List<PluginCatalogContributionEntity> findByPluginId(UUID pluginId);
}
