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
package com.yorel.muxon.services.plugin;

import com.yorel.muxon.api.enums.CatalogContributionStatus;
import com.yorel.muxon.db.model.PluginCatalogContributionEntity;
import com.yorel.muxon.db.repository.PluginCatalogContributionRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages admin approval of plugin-contributed catalog items. Only active when {@code
 * muxon.plugins.catalog.approval-required=true}.
 */
@Service
@ConditionalOnProperty(name = "muxon.plugins.catalog.approval-required", havingValue = "true")
public class CatalogApprovalService {

  private static final Logger log = LoggerFactory.getLogger(CatalogApprovalService.class);

  @Autowired private PluginCatalogContributionRepository catalogContributionRepository;

  @Transactional
  public PluginCatalogContributionEntity approveItem(UUID itemId) {
    PluginCatalogContributionEntity item = require(itemId);
    if (item.getStatus() != CatalogContributionStatus.PENDING_APPROVAL) {
      throw new IllegalStateException("Item " + itemId + " is not in PENDING_APPROVAL state");
    }
    item.setStatus(CatalogContributionStatus.ACTIVE);
    item = catalogContributionRepository.save(item);
    log.info("Catalog item approved: {} id={}", item.getName(), itemId);
    return item;
  }

  @Transactional
  public PluginCatalogContributionEntity rejectItem(UUID itemId, String reason) {
    PluginCatalogContributionEntity item = require(itemId);
    item.setStatus(CatalogContributionStatus.HIDDEN);
    item = catalogContributionRepository.save(item);
    log.info("Catalog item rejected: {} id={} reason={}", item.getName(), itemId, reason);
    return item;
  }

  private PluginCatalogContributionEntity require(UUID itemId) {
    return catalogContributionRepository
        .findById(itemId)
        .orElseThrow(() -> new IllegalArgumentException("Catalog item not found: " + itemId));
  }
}
