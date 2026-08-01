/*
 * Copyright 2026 Yorel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yorel.muxon.db.repository;

import com.yorel.muxon.db.model.ContentItemDistributionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentItemDistributionRepository
    extends JpaRepository<ContentItemDistributionEntity, UUID> {
  List<ContentItemDistributionEntity> findByDistributionIdOrderByContentItemId(UUID distributionId);

  Page<ContentItemDistributionEntity> findByDistributionId(UUID distributionId, Pageable pageable);

  Page<ContentItemDistributionEntity> findByDistributionIdAndStatus(
      UUID distributionId, String status, Pageable pageable);

  Optional<ContentItemDistributionEntity> findByDistributionIdAndContentItemId(
      UUID distributionId, UUID contentItemId);

  long countByDistributionId(UUID distributionId);

  long countByDistributionIdAndStatus(UUID distributionId, String status);

  void deleteByDistributionId(UUID distributionId);
}
