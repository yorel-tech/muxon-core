package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ContentStorageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContentStorageRepository extends JpaRepository<ContentStorageEntity, UUID> {

    Optional<ContentStorageEntity> findByDefaultStorageIsTrue();

    Page<ContentStorageEntity> findAllByOrderByNameAsc(Pageable pageable);
}
