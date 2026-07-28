package com.scal.muxon.db.repository;

import com.scal.muxon.api.enums.PluginSource;
import com.scal.muxon.api.enums.PluginStatus;
import com.scal.muxon.db.model.PluginEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginRepository extends JpaRepository<PluginEntity, UUID> {

    Optional<PluginEntity> findByNameAndVersion(String name, String version);

    List<PluginEntity> findByStatus(PluginStatus status);

    List<PluginEntity> findBySource(PluginSource source);

    boolean existsByNameAndVersion(String name, String version);
}
