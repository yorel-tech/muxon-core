package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {
}
