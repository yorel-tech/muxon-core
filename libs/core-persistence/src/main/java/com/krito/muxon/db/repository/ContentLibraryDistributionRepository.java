package com.krito.muxon.db.repository;

import com.krito.muxon.db.model.ContentLibraryDistributionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentLibraryDistributionRepository extends JpaRepository<ContentLibraryDistributionEntity, UUID> {
    List<ContentLibraryDistributionEntity> findByLibraryIdOrderByDatacenterId(UUID libraryId);

    Optional<ContentLibraryDistributionEntity> findByLibraryIdAndDatacenterId(UUID libraryId, UUID datacenterId);

    void deleteByLibraryIdAndDatacenterId(UUID libraryId, UUID datacenterId);

    List<ContentLibraryDistributionEntity> findByDatacenterId(UUID datacenterId);
}
