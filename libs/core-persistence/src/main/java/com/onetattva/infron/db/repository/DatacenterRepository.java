package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Datacenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DatacenterRepository extends JpaRepository<Datacenter, java.util.UUID> {

    // Custom queries can be added here if needed
    // Example: List<Datacenter> findByName(String name);
}
