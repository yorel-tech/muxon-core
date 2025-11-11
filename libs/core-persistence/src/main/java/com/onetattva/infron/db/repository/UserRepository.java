package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    // Custom queries can be added here if needed
    // Example: List<UserEntity> findByName(String name);
}
