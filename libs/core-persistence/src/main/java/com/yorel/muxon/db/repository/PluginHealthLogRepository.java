package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.PluginHealthLogEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PluginHealthLogRepository extends JpaRepository<PluginHealthLogEntity, UUID> {

    @Query("SELECT h FROM PluginHealthLogEntity h WHERE h.plugin.id = :pluginId ORDER BY h.checkedAt DESC")
    List<PluginHealthLogEntity> findTopNByPluginIdOrderByCheckedAtDesc(
            @Param("pluginId") UUID pluginId, PageRequest pageable);
}
