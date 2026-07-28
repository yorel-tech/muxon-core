package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.NodeClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NodeClusterRepository extends JpaRepository<NodeClusterEntity, java.util.UUID> {

    List<NodeClusterEntity> findByProvider_Id(UUID providerId);
}
