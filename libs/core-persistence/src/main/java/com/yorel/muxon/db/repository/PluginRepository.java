package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.enums.PluginSource;
import com.yorel.muxon.api.enums.PluginStatus;
import com.yorel.muxon.db.model.PluginEntity;
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
