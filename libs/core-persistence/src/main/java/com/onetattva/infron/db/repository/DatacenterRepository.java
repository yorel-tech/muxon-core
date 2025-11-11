package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.DatacenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatacenterRepository extends JpaRepository<DatacenterEntity, java.util.UUID> {

    // Custom queries can be added here if needed
    // Example: List<DatacenterEntity> findByName(String name);
}
