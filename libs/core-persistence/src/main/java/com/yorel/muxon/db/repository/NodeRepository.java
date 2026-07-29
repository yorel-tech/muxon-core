package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.NodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<NodeEntity, java.util.UUID> {

    /**
     * Find all nodes for a given provider.
     */
    List<NodeEntity> findByProviderId(UUID providerId);

    Optional<NodeEntity> findByProvider_IdAndExternalId(UUID providerId, String externalId);
}
