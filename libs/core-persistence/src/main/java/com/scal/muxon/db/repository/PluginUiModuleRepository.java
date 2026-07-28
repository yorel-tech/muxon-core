package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.PluginUiModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PluginUiModuleRepository extends JpaRepository<PluginUiModuleEntity, UUID> {

    List<PluginUiModuleEntity> findByPluginIdIn(List<UUID> pluginIds);

    List<PluginUiModuleEntity> findByPluginId(UUID pluginId);
}
