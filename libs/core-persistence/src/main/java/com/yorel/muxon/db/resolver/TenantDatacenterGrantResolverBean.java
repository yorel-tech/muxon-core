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
package com.yorel.muxon.db.resolver;

import com.yorel.muxon.api.model.ProviderType;
import com.yorel.muxon.db.model.DatacenterEntity;
import com.yorel.muxon.db.model.TenantDatacenterGrantEntity;
import com.yorel.muxon.db.repository.TenantDatacenterGrantRepository;
import com.yorel.muxon.providers.TenantDatacenterGrantResolver;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Resolves the VM provider ID for a tenant datacenter grant by loading grant and datacenter from
 * the database.
 */
@Service
public class TenantDatacenterGrantResolverBean implements TenantDatacenterGrantResolver {

  private static final Logger logger =
      LoggerFactory.getLogger(TenantDatacenterGrantResolverBean.class);

  @Autowired private TenantDatacenterGrantRepository tenantDatacenterGrantRepository;

  @Override
  public Optional<String> resolveProviderId(UUID tenantDatacenterGrantId) {
    Optional<TenantDatacenterGrantEntity> grantOpt =
        tenantDatacenterGrantRepository.findById(tenantDatacenterGrantId);
    if (grantOpt.isEmpty()) {
      logger.warn("No tenant datacenter grant found for ID: {}", tenantDatacenterGrantId);
      return Optional.empty();
    }

    TenantDatacenterGrantEntity grant = grantOpt.get();
    DatacenterEntity datacenter = grant.getDatacenter();

    if (datacenter == null) {
      logger.warn("Datacenter not found for grant ID: {}", tenantDatacenterGrantId);
      return Optional.empty();
    }

    if (datacenter.getNodeCluster() != null && datacenter.getNodeCluster().getProvider() != null) {
      return Optional.of(datacenter.getNodeCluster().getProvider().getId().toString());
    }

    if (datacenter.getSettings() != null) {
      ProviderType providerType = datacenter.getSettings().getProviderType();
      return Optional.of(mapProviderTypeToProviderId(providerType));
    }

    logger.warn("No provider linked to datacenter for grant ID: {}", tenantDatacenterGrantId);
    return Optional.empty();
  }

  private static String mapProviderTypeToProviderId(ProviderType providerType) {
    if (providerType == null) {
      return "mock";
    }
    return switch (providerType) {
      case PROXMOX, LIBVIRT -> "mock";
      default -> "mock";
    };
  }
}
