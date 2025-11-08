package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Custom queries can be added here if needed
    // Example: List<User> findByName(String name);
}
