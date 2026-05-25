package com.sal.muxon.services;

import com.sal.muxon.api.model.ComputeProfile;
import com.sal.muxon.api.model.ComputeProfileCreate;
import com.sal.muxon.api.model.ComputeProfileList;
import com.sal.muxon.api.model.ComputeProfileSpec;
import com.sal.muxon.api.model.ComputeProfileUpdate;
import com.sal.muxon.api.model.Spec;
import com.sal.muxon.db.model.ComputeProfileEntity;
import com.sal.muxon.db.repository.ComputeProfileRepository;
import com.sal.muxon.db.repository.VmRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for compute profile (VM spec template) operations.
 */
@Service
public class ComputeProfileService {

    @Autowired
    private ComputeProfileRepository computeProfileRepository;

    @Autowired
    private VmRepository vmRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Create a new compute profile
     */
    @Transactional
    public ComputeProfile createProfile(ComputeProfileCreate request) {
        ComputeProfileEntity entity = new ComputeProfileEntity();
        entity.setTenantDatacenterGrantId(request.getTenantDatacenterGrantId());
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setSpec(specToJson(request.getSpec()));
        entity.setMetadata(request.getMetadata());
        entity.setTags(request.getTags());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        entity = computeProfileRepository.save(entity);
        return toApiModel(entity);
    }

    /**
     * Get a compute profile by ID
     */
    public ComputeProfile getProfile(UUID profileId) {
        ComputeProfileEntity entity = computeProfileRepository.findById(profileId)
                .orElseThrow(() -> new com.sal.muxon.common.EntityNotFoundException(
                        "Compute profile not found: " + profileId));
        return toApiModel(entity);
    }

    /**
     * List compute profiles with filters
     */
    public ComputeProfileList listProfiles(Integer page, Integer perPage, String sort,
                                       UUID tenantDatacenterGrantId, String name, List<String> tags) {
        Pageable pageable = PageRequest.of(page - 1, perPage,
                Sort.by(sort != null ? sort : "name"));
        
        Page<ComputeProfileEntity> entityPage;
        
        if (tenantDatacenterGrantId != null) {
            entityPage = computeProfileRepository.findByTenantDatacenterGrantId(
                    tenantDatacenterGrantId, pageable);
        } else if (name != null) {
            entityPage = computeProfileRepository.findByNameContaining(name, pageable);
        } else if (tags != null && !tags.isEmpty()) {
            entityPage = computeProfileRepository.findByTags(tags, pageable);
        } else {
            // When no filters, include system profiles
            List<ComputeProfile> allProfiles = getAllProfiles();
            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, allProfiles.size());
            
            ComputeProfileList response = new ComputeProfileList();
            response.setItems(allProfiles.subList(start, end));
            response.setTotal(allProfiles.size());
            response.setPage(page);
            response.setPerPage(perPage);
            response.setTotalPages((int) Math.ceil((double) allProfiles.size() / perPage));
            return response;
        }
        
        List<ComputeProfile> profiles = entityPage.getContent().stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());
        
        ComputeProfileList response = new ComputeProfileList();
        response.setItems(profiles);
        response.setTotal((int) entityPage.getTotalElements());
        response.setPage(page);
        response.setPerPage(perPage);
        response.setTotalPages(entityPage.getTotalPages());
        
        return response;
    }

    /**
     * Update a compute profile
     */
    @Transactional
    public ComputeProfile updateProfile(UUID profileId, ComputeProfileUpdate request) {
        ComputeProfileEntity entity = computeProfileRepository.findById(profileId)
                .orElseThrow(() -> new com.sal.muxon.common.EntityNotFoundException(
                        "Compute profile not found: " + profileId));
        
        if (request.getName() != null) {
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) {
            entity.setDescription(request.getDescription());
        }
        if (request.getSpec() != null) {
            entity.setSpec(specToJson(request.getSpec()));
        }
        if (request.getMetadata() != null) {
            entity.setMetadata(request.getMetadata());
        }
        if (request.getTags() != null) {
            entity.setTags(request.getTags());
        }
        entity.setUpdatedAt(Instant.now());
        
        entity = computeProfileRepository.save(entity);
        return toApiModel(entity);
    }

    /**
     * Delete a compute profile
     */
    @Transactional
    public void deleteProfile(UUID profileId) {
        if (!computeProfileRepository.existsById(profileId)) {
            throw new com.sal.muxon.common.EntityNotFoundException(
                    "Compute profile not found: " + profileId);
        }
        computeProfileRepository.deleteById(profileId);
    }

    /**
     * Get metadata for a compute profile
     */
    public java.util.Map<String, String> getMetadata(UUID profileId) {
        ComputeProfileEntity entity = computeProfileRepository.findById(profileId)
                .orElseThrow(() -> new com.sal.muxon.common.EntityNotFoundException(
                        "Compute profile not found: " + profileId));
        return entity.getMetadata();
    }

    /**
     * Update metadata for a compute profile
     */
    @Transactional
    public java.util.Map<String, String> updateMetadata(UUID profileId,
            java.util.Map<String, String> metadata) {
        ComputeProfileEntity entity = computeProfileRepository.findById(profileId)
                .orElseThrow(() -> new com.sal.muxon.common.EntityNotFoundException(
                        "Compute profile not found: " + profileId));
        entity.setMetadata(metadata);
        entity.setUpdatedAt(Instant.now());
        entity = computeProfileRepository.save(entity);
        return entity.getMetadata();
    }

    /**
     * Get profiles for a tenant datacenter grant
     */
    public List<ComputeProfile> getProfilesForTenantDatacenter(UUID tenantDatacenterGrantId) {
        return computeProfileRepository.findByTenantDatacenterGrantId(
                        tenantDatacenterGrantId, PageRequest.of(0, 100))
                .getContent().stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());
    }

    /**
     * Get system profiles (available to all tenants)
     */
    public List<ComputeProfile> getSystemProfiles() {
        return computeProfileRepository.findSystemProfiles().stream()
                .map(this::toApiModel)
                .collect(Collectors.toList());
    }

    /**
     * Get all profiles (tenant + system)
     */
    public List<ComputeProfile> getAllProfiles() {
        List<ComputeProfile> tenantProfiles = getProfilesForTenantDatacenter(null);
        List<ComputeProfile> systemProfiles = getSystemProfiles();
        tenantProfiles.addAll(systemProfiles);
        return tenantProfiles;
    }

    /**
     * Convert entity to API model
     */
    private ComputeProfile toApiModel(ComputeProfileEntity entity) {
        ComputeProfile model = new ComputeProfile();
        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setDescription(entity.getDescription());
        model.setTenantDatacenterGrantId(entity.getTenantDatacenterGrantId());
        model.setSpec(jsonToSpec(entity.getSpec()));
        model.setMetadata(entity.getMetadata());
        model.setTags(entity.getTags());
        model.setCreatedAt(instantToOffsetDateTime(entity.getCreatedAt()));
        model.setUpdatedAt(instantToOffsetDateTime(entity.getUpdatedAt()));
        model.setIsSystem(entity.isSystem());
        return model;
    }

    /**
     * Convert Spec object to JSON string
     */
    private String specToJson(Spec spec) {
        try {
            return objectMapper.writeValueAsString(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert spec to JSON", e);
        }
    }

    /**
     * Convert JSON string to ComputeProfileSpec object
     */
    private ComputeProfileSpec jsonToSpec(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ComputeProfileSpec.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to spec", e);
        }
    }

    /**
     * Convert Instant to OffsetDateTime
     */
    private OffsetDateTime instantToOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atOffset(ZoneOffset.UTC);
    }

    public Map<String, String> getEntityMetadata(@NotNull UUID profileId) {
        return null;
    }

    public Map<String, String> updateEntityMetadata(@NotNull UUID profileId, @Valid Map<String, String> requestBody) {
        return null;
    }
}
