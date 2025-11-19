package com.onetattva.infron.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.model.ProjectEntity;
import com.onetattva.infron.db.model.TenantEntity;
import com.onetattva.infron.db.model.TenantUserEntity;
import com.onetattva.infron.db.repository.ProjectRepository;
import com.onetattva.infron.db.repository.TenantRepository;
import com.onetattva.infron.db.repository.TenantUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    public Project createProject(UUID tenantId, ProjectCreate projectCreate) {
        TenantEntity tenant = tenantRepository.findById(tenantId).orElseThrow();
        ProjectEntity entityProject = new ProjectEntity();
        entityProject.setId(UUID.randomUUID());
        entityProject.setTenant(tenant);
        entityProject.setName(projectCreate.getName());
        entityProject.setDisplayName(projectCreate.getDisplayName());
        entityProject.setDescription(projectCreate.getDescription());
        entityProject.setStatus(com.onetattva.infron.db.ProjectStatus.ACTIVE);
        if (projectCreate.getOwner() != null) {
            TenantUserEntity owner = tenantUserRepository
                    .findById(projectCreate.getOwner().getId()).orElseThrow();
            entityProject.setOwner(owner);
        }
        entityProject.setResourceLimits(projectCreate.getResourceLimits());
        Instant now = Instant.now();
        entityProject.setCreatedAt(now);
        entityProject.setUpdatedAt(now);
        ProjectEntity saved = projectRepository.save(entityProject);
        return mapEntityToApi(saved);
    }

    public Project getProject(UUID tenantId, UUID projectId) {
        ProjectEntity entity = projectRepository.findById(projectId)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public void deleteProject(UUID tenantId, UUID projectId) {
        projectRepository.findById(projectId)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .ifPresent(projectRepository::delete);
    }

    private Project mapEntityToApi(ProjectEntity entity) {
        Project api = new Project();
        api.setId(entity.getId());
        api.setTenant(new EntityReference().id(entity.getTenant().getId()).name(entity.getTenant().getDisplayName()));
        api.setName(entity.getName());
        api.setDisplayName(entity.getDisplayName());
        api.setDescription(entity.getDescription());
        api.setStatus(Project.StatusEnum.fromValue(entity.getStatus().toString().toLowerCase()));
        if (entity.getOwner() != null) {
            api.setOwner(
                    new EntityReference().id(entity.getOwner().getId()).name(entity.getOwner().getUsername()));
        }
        api.setResourceLimits(entity.getResourceLimits());
        api.setCreatedAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return api;
    }

    private com.onetattva.infron.db.ProjectStatus mapApiStatus(Project.StatusEnum status) {
        return com.onetattva.infron.db.ProjectStatus.valueOf(status.name());
    }

    public ProjectList listProjects(UUID tenantId, Integer page, Integer perPage) {
        if (page == null) {
            page = 1;
        }
        if (perPage == null) {
            perPage = 20;
        }
        // Clamp perPage to be between 1 and 200
        if (perPage < 1) {
            perPage = 1;
        }
        if (perPage > 200) {
            perPage = 200;
        }
        Pageable pageable = PageRequest.of(page - 1, perPage);
        var entities = projectRepository.findByTenant_Id(tenantId, pageable);
        ProjectList result = new ProjectList();
        result.setTotal((int) entities.getTotalElements());
        result.setPage(page);
        result.setPerPage(perPage);
        result.setItems(entities.getContent().stream().map(this::mapEntityToApi).collect(Collectors.toList()));
        return result;
    }
}
