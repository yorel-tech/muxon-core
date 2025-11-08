package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, java.util.UUID> {

}
