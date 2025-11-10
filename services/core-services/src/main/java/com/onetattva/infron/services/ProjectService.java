package com.onetattva.infron.services;

import com.onetattva.infron.api.model.*;
import com.onetattva.infron.db.repository.ProjectRepository;
import com.onetattva.infron.db.repository.TenantRepository;
import com.onetattva.infron.db.repository.TenantUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantUserRepository tenantUserRepository;

    public Project createProject(UUID tenantId, ProjectCreate projectCreate) {
        com.onetattva.infron.db.model.Tenant tenant = tenantRepository.findById(tenantId).orElseThrow();
        com.onetattva.infron.db.model.Project entityProject = new com.onetattva.infron.db.model.Project();
        entityProject.setId(UUID.randomUUID());
        entityProject.setTenant(tenant);
        entityProject.setName(projectCreate.getName());
        entityProject.setDisplayName(projectCreate.getDisplayName());
        entityProject.setDescription(projectCreate.getDescription());
        entityProject.setLabels(projectCreate.getLabels());
        entityProject.setStatus(com.onetattva.infron.db.ProjectStatus.ACTIVE);
        if (projectCreate.getOwner() != null) {
            com.onetattva.infron.db.model.TenantUser owner = tenantUserRepository
                    .findById(projectCreate.getOwner().getId()).orElseThrow();
            entityProject.setOwner(owner);
        }
        entityProject.setResourceLimits(projectCreate.getResourceLimits());
        Instant now = Instant.now();
        entityProject.setCreatedAt(now);
        entityProject.setUpdatedAt(now);
        com.onetattva.infron.db.model.Project saved = projectRepository.save(entityProject);
        return mapEntityToApi(saved);
    }

    public Project getProject(UUID tenantId, UUID projectId) {
        com.onetattva.infron.db.model.Project entity = projectRepository.findById(projectId)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .orElseThrow();
        return mapEntityToApi(entity);
    }

    public void deleteProject(UUID tenantId, UUID projectId) {
        projectRepository.findById(projectId)
                .filter(p -> p.getTenant().getId().equals(tenantId))
                .ifPresent(projectRepository::delete);
    }

    private Project mapEntityToApi(com.onetattva.infron.db.model.Project entity) {
        Project api = new Project();
        api.setId(entity.getId());
        api.setTenant(new EntityReference().id(entity.getTenant().getId()).name(entity.getTenant().getDisplayName()));
        api.setName(entity.getName());
        api.setDisplayName(entity.getDisplayName());
        api.setDescription(entity.getDescription());
        api.setLabels(entity.getLabels());
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
}
