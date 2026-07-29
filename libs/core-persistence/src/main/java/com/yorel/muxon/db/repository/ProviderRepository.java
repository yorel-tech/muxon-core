package com.yorel.muxon.db.repository;

import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.db.model.ProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

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

    /**
     * Minimal projection for provider connection polling.
     */
    @Query("SELECT p.status AS status, p.metadata AS metadata, p.capabilities AS capabilities FROM ProviderEntity p WHERE p.id = :id")
    Optional<ProviderConnectionStateProjection> findConnectionStateById(@Param("id") UUID id);

    interface ProviderConnectionStateProjection {
        String getStatus();
        java.util.Map<String, String> getMetadata();
        java.util.Map<String, String> getCapabilities();
    }
}
