package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.NodeCluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeClusterRepository extends JpaRepository<NodeCluster, java.util.UUID> {

}
