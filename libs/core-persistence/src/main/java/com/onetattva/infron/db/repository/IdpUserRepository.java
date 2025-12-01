package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.IdpUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdpUserRepository extends JpaRepository<IdpUserEntity, java.util.UUID> {

}
