package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ContentItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItemEntity, UUID> {
    Page<ContentItemEntity> findByLibraryId(UUID libraryId, Pageable pageable);
    Page<ContentItemEntity> findByLibraryIdAndContentType(UUID libraryId, String contentType, Pageable pageable);
    Page<ContentItemEntity> findByContentType(String contentType, Pageable pageable);
}
