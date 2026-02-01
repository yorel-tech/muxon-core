package com.onetattva.infron.db.repository;

import com.onetattva.infron.api.model.ProviderType;
import com.onetattva.infron.db.model.ProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {
    
    /**
     * Find providers by type
     */
    List<ProviderEntity> findByType(ProviderType type);
    
    /**
     * Find providers by status
     */
    List<ProviderEntity> findByStatus(String status);
    
    /**
     * Find provider by type and status
     */
    List<ProviderEntity> findByTypeAndStatus(ProviderType type, String status);
    
    /**
     * Find provider by name (case-insensitive)
     */
    Optional<ProviderEntity> findByNameIgnoreCase(String name);
    
    /**
     * Check if a provider with the same name exists (excluding current provider)
     */
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
