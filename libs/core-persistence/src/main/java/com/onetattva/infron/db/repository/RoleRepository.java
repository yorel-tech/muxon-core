package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, java.util.UUID> {

}
