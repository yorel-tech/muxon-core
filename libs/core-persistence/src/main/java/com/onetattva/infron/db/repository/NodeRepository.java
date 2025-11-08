package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NodeRepository extends JpaRepository<Node, java.util.UUID> {

}
