package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, java.util.UUID> {

    List<Project> findByTenant_Id(UUID tenantId);

}
