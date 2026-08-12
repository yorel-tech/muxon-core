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
package com.yorel.muxon.services;

import com.yorel.muxon.api.enums.TenantStatus;
import com.yorel.muxon.api.model.Tenant;
import com.yorel.muxon.api.model.TenantCreate;
import com.yorel.muxon.api.model.TenantList;
import com.yorel.muxon.api.model.TenantSettings;
import com.yorel.muxon.api.model.TenantUpdate;
import com.yorel.muxon.auth.AuthorizationService;
import com.yorel.muxon.common.Constants;
import com.yorel.muxon.common.EntityNotFoundException;
import com.yorel.muxon.db.model.TenantEntity;
import com.yorel.muxon.db.repository.TenantRepository;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TenantsService {

  @Autowired private TenantRepository tenantRepository;

  @Autowired private AuthorizationService authorizationService;

  @Autowired(required = false)
  private TenantNetworkPolicyService tenantNetworkPolicyService;

  public static final String RESERVED_TENANT_NAME = "system";

  public Tenant createTenant(TenantCreate tenantCreate) {
    String name = tenantCreate.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tenant name is required");
    }
    String nameNormalized = name.trim().toLowerCase();
    if (RESERVED_TENANT_NAME.equals(nameNormalized)) {
      throw new IllegalArgumentException("Tenant name '" + name + "' is reserved for system use");
    }
    if (tenantRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
      throw new IllegalArgumentException("Tenant with name '" + name + "' already exists");
    }

    TenantEntity entityTenant = new TenantEntity();
    entityTenant.setId(UUID.randomUUID());
    entityTenant.setName(name.trim());
    entityTenant.setDisplayName(tenantCreate.getDisplayName());
    entityTenant.setStatus(TenantStatus.ACTIVE);
    try {
      entityTenant.setMetadata(tenantCreate.getMetadata());
    } catch (Exception e) {
      entityTenant.setMetadata(Map.of());
    }
    Instant now = Instant.now();
    entityTenant.setCreatedAt(now);
    entityTenant.setUpdatedAt(now);
    TenantEntity saved = tenantRepository.save(entityTenant);

    // Auto-create default tenant network policy
    if (tenantNetworkPolicyService != null) {
      tenantNetworkPolicyService.createDefaultPolicy(saved);
    }

    return mapEntityToApi(saved);
  }

  public void deleteTenant(UUID tenantId) {
    if (!tenantRepository.existsById(tenantId)) {
      throw new EntityNotFoundException("Tenant not found: " + tenantId);
    }
    tenantRepository.deleteById(tenantId);
  }

  public Tenant getTenant(UUID tenantId) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
    return mapEntityToApi(entity);
  }

  /**
   * Resolves the current tenant for the authenticated user. Used by the tenant portal so the UI
   * gets tenantId without calling the provider-scoped list tenants API.
   *
   * @param externalId JWT subject (user external id)
   * @param slugOptional Tenant name/slug from login; when provided, returns that tenant if the user
   *     has access
   * @return the tenant the user is acting in
   * @throws EntityNotFoundException if no tenant can be resolved (e.g. user has no tenants, or slug
   *     not found / no access)
   */
  public Tenant getCurrentTenantForUser(String externalId, String slugOptional) {
    List<String> allowedTenantIds = authorizationService.getTenantsForExternalId(externalId);
    if (allowedTenantIds == null || allowedTenantIds.isEmpty()) {
      throw new EntityNotFoundException("No tenant found for current user");
    }
    if (slugOptional != null && !slugOptional.isBlank()) {
      String slug = slugOptional.trim().toLowerCase();
      TenantEntity byName = tenantRepository.findByNameIgnoreCase(slug).orElse(null);
      if (byName == null || !allowedTenantIds.contains(byName.getId().toString())) {
        throw new EntityNotFoundException("Tenant not found or access denied");
      }
      return mapEntityToApi(byName);
    }
    if (allowedTenantIds.size() == 1) {
      UUID tenantId = UUID.fromString(allowedTenantIds.get(0));
      return getTenant(tenantId);
    }
    throw new EntityNotFoundException("Multiple tenants: provide slug query param");
  }

  public TenantList getTenantsForCurrentUser(String externalId) {
    List<String> allowedTenantIds = authorizationService.getTenantsForExternalId(externalId);
    List<Tenant> tenants =
        (allowedTenantIds == null ? List.<String>of() : allowedTenantIds)
            .stream().map(UUID::fromString).map(this::getTenant).toList();

    TenantList tenantList = new TenantList();
    tenantList.setTotal(tenants.size());
    tenantList.setPage(1);
    tenantList.setPerPage(tenants.size());
    tenantList.setItems(tenants);
    return tenantList;
  }

  public TenantList listTenants(
      Integer page, Integer perPage, String sort, String name, String status) {
    // TODO: Implement full filters (name, status) and sorting
    List<TenantEntity> allNonSystem =
        tenantRepository.findAll().stream()
            .filter(e -> !Constants.SYSTEM_ID.equals(e.getId().toString()))
            .toList();
    int total = allNonSystem.size();
    int from = Math.max(0, (page - 1) * perPage);
    List<TenantEntity> pageEntities = allNonSystem.stream().skip(from).limit(perPage).toList();
    List<Tenant> apiTenants = pageEntities.stream().map(this::mapEntityToApi).toList();
    TenantList tenantList = new TenantList();
    tenantList.setTotal(total);
    tenantList.setPage(page);
    tenantList.setPerPage(perPage);
    tenantList.setItems(apiTenants);
    return tenantList;
  }

  public Tenant patchTenant(UUID tenantId, TenantUpdate tenantUpdate) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
    if (tenantUpdate.getDisplayName() != null) {
      entity.setDisplayName(tenantUpdate.getDisplayName());
    }
    if (tenantUpdate.getStatus() != null) {
      entity.setStatus(mapUpdateStatus(tenantUpdate.getStatus()));
    }
    if (tenantUpdate.getMetadata() != null) {
      try {
        entity.setMetadata(tenantUpdate.getMetadata());
      } catch (Exception e) {
        // keep existing
      }
    }
    entity.setUpdatedAt(Instant.now());
    TenantEntity saved = tenantRepository.save(entity);
    return mapEntityToApi(saved);
  }

  public Tenant updateTenant(UUID tenantId, TenantUpdate tenantUpdate) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found: " + tenantId));
    if (tenantUpdate.getDisplayName() != null) {
      entity.setDisplayName(tenantUpdate.getDisplayName());
    }
    if (tenantUpdate.getStatus() != null) {
      entity.setStatus(mapUpdateStatus(tenantUpdate.getStatus()));
    }
    try {
      entity.setMetadata(tenantUpdate.getMetadata());
    } catch (Exception e) {
      // ignore
    }
    entity.setUpdatedAt(Instant.now());
    TenantEntity saved = tenantRepository.save(entity);
    return mapEntityToApi(saved);
  }

  public TenantSettings getTenantSettings(UUID tenantId) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    return entity.getSettings();
  }

  public TenantSettings replaceTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    // TODO: Serialize settings to JSON and save
    entity.setUpdatedAt(Instant.now());
    tenantRepository.save(entity);
    return tenantSettings;
  }

  public TenantSettings updateTenantSettings(UUID tenantId, TenantSettings tenantSettings) {
    TenantEntity entity =
        tenantRepository
            .findById(tenantId)
            .orElseThrow(() -> new EntityNotFoundException("Tenant not found"));
    // TODO: Merge settings JSON and save
    entity.setUpdatedAt(Instant.now());
    tenantRepository.save(entity);
    return tenantSettings;
  }

  private Tenant mapEntityToApi(TenantEntity entity) {
    Tenant api = new Tenant();
    api.setId(entity.getId());
    api.setName(entity.getName());
    api.setDisplayName(entity.getDisplayName());
    api.setStatus(Tenant.StatusEnum.fromValue(entity.getStatus().toString().toLowerCase()));
    try {
      api.setMetadata(entity.getMetadata());
    } catch (Exception e) {
      api.setMetadata(Map.of());
    }
    if (entity.getCreatedAt() != null) {
      api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
    if (entity.getUpdatedAt() != null) {
      api.setUpdatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    return api;
  }

  private TenantStatus mapApiStatus(Tenant.StatusEnum status) {
    return TenantStatus.valueOf(status.name());
  }

  private TenantStatus mapUpdateStatus(TenantUpdate.StatusEnum status) {
    return TenantStatus.valueOf(status.name());
  }
}
