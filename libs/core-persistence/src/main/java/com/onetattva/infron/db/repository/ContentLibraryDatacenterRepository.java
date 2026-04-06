package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ContentLibraryDatacenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentLibraryDatacenterRepository extends JpaRepository<ContentLibraryDatacenterEntity, UUID> {
    List<ContentLibraryDatacenterEntity> findByLibraryIdOrderByDatacenterId(UUID libraryId);

    Optional<ContentLibraryDatacenterEntity> findByLibraryIdAndDatacenterId(UUID libraryId, UUID datacenterId);

    void deleteByLibraryIdAndDatacenterId(UUID libraryId, UUID datacenterId);

    List<ContentLibraryDatacenterEntity> findByDatacenterId(UUID datacenterId);
}
