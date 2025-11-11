package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, java.util.UUID> {

    List<ProjectEntity> findByTenant_Id(UUID tenantId);

}
