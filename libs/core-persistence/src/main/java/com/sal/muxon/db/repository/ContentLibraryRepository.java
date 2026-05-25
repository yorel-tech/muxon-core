package com.sal.muxon.db.repository;

import com.sal.muxon.db.model.ContentLibraryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ContentLibraryRepository extends JpaRepository<ContentLibraryEntity, UUID> {
    Page<ContentLibraryEntity> findByTenantId(UUID tenantId, Pageable pageable);

    Page<ContentLibraryEntity> findByTenantIdIn(Collection<UUID> tenantIds, Pageable pageable);

    Optional<ContentLibraryEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<ContentLibraryEntity> findByIdAndTenantIdIn(UUID id, Collection<UUID> tenantIds);

    long countByContentStorageId(UUID contentStorageId);
}
