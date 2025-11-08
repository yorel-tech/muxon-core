package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, java.util.UUID> {

}
