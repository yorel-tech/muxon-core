package com.onetattva.infron.db.repository;

import com.onetattva.infron.db.model.ContentItemDistributionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContentItemDistributionRepository extends JpaRepository<ContentItemDistributionEntity, UUID> {
    List<ContentItemDistributionEntity> findByDistributionIdOrderByContentItemId(UUID distributionId);

    Page<ContentItemDistributionEntity> findByDistributionId(UUID distributionId, Pageable pageable);

    Page<ContentItemDistributionEntity> findByDistributionIdAndStatus(UUID distributionId, String status, Pageable pageable);

    Optional<ContentItemDistributionEntity> findByDistributionIdAndContentItemId(UUID distributionId, UUID contentItemId);

    long countByDistributionId(UUID distributionId);

    long countByDistributionIdAndStatus(UUID distributionId, String status);

    void deleteByDistributionId(UUID distributionId);
}
