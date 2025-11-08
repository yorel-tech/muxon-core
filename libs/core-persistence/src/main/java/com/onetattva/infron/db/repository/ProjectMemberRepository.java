package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, java.util.UUID> {

}
