package com.scal.muxon.db.repository;

import com.scal.muxon.db.model.ContentItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ContentItemRepository extends JpaRepository<ContentItemEntity, UUID> {
    List<ContentItemEntity> findByLibraryId(UUID libraryId);

    Page<ContentItemEntity> findByLibraryId(UUID libraryId, Pageable pageable);
    Page<ContentItemEntity> findByLibraryIdAndContentType(UUID libraryId, String contentType, Pageable pageable);
    Page<ContentItemEntity> findByContentType(String contentType, Pageable pageable);
}
