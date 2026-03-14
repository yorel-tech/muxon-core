package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.NodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NodeRepository extends JpaRepository<NodeEntity, java.util.UUID> {

    /**
     * Find all nodes for a given provider.
     */
    List<NodeEntity> findByProviderId(UUID providerId);
}
