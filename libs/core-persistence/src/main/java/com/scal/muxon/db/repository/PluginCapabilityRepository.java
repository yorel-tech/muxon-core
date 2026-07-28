package com.scal.muxon.db.repository;

import com.scal.muxon.api.enums.PluginCapabilityType;
import com.scal.muxon.db.model.PluginCapabilityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PluginCapabilityRepository extends JpaRepository<PluginCapabilityEntity, UUID> {

    List<PluginCapabilityEntity> findByPluginIdAndCapabilityType(UUID pluginId, PluginCapabilityType capabilityType);

    List<PluginCapabilityEntity> findByPluginId(UUID pluginId);
}
