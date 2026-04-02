package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ContentLibraryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContentLibraryRepository extends JpaRepository<ContentLibraryEntity, UUID> {
    Page<ContentLibraryEntity> findByScope(String scope, Pageable pageable);
    Page<ContentLibraryEntity> findByTenantIdOrScope(UUID tenantId, String scope, Pageable pageable);
    Optional<ContentLibraryEntity> findByIdAndScope(UUID id, String scope);
    Optional<ContentLibraryEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}
