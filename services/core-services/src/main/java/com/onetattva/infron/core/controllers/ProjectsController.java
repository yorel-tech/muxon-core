package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.ProjectsApi;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.api.model.*;
import com.onetattva.infron.core.auth.Permission;
import com.onetattva.infron.core.auth.RequiresPermission;
import com.onetattva.infron.core.services.ProjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProjectsController implements ProjectsApi {

    @Autowired
    private ProjectService projectService;

    @Override
    @RequiresPermission(Permission.PROJECT_MANAGE)
    public ResponseEntity<ProjectMember> addProjectMember(UUID tenantId, UUID projectId, ProjectMember projectMember) {
        // TODO: Implement add project member logic
        ProjectMember newMember = new ProjectMember();
        newMember.setUserId(projectMember.getUserId());
        newMember.setRole(projectMember.getRole());
        return ResponseEntity.status(201).body(newMember);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_MANAGE)
    public ResponseEntity<Project> createProject(UUID tenantId, ProjectCreate projectCreate) {
        Project project = projectService.createProject(tenantId, projectCreate);
        return ResponseEntity.status(201).body(project);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_MANAGE)
    public ResponseEntity<Void> deleteProject(UUID tenantId, UUID projectId) {
        projectService.deleteProject(tenantId, projectId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.PROJECT_READ)
    public ResponseEntity<Project> getProject(UUID tenantId, UUID projectId) {
        Project project = projectService.getProject(tenantId, projectId);
        return ResponseEntity.ok(project);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_READ)
    public ResponseEntity<ListProjectMembers200Response> listProjectMembers(UUID tenantId, UUID projectId) {
        // TODO: Implement list project members logic
        ListProjectMembers200Response response = new ListProjectMembers200Response();
        response.setTotal(1);
        // Add dummy members
        ProjectMember member = new ProjectMember();
        // Skip setting userId and role for dummy
        response.setItems(List.of(member));
        return ResponseEntity.ok(response);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_READ)
    public ResponseEntity<ProjectList> listProjects(UUID tenantId, Integer page, Integer perPage) {
        ProjectList projectList = projectService.listProjects(tenantId, page, perPage);;
        return ResponseEntity.ok(projectList);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_MANAGE)
    public ResponseEntity<Void> removeProjectMember(UUID tenantId, UUID projectId, UUID userId) {
        // TODO: Implement remove project member logic
        return ResponseEntity.noContent().build();
    }

    @Override
    @RequiresPermission(Permission.PROJECT_EDIT)
    public ResponseEntity<Project> replaceProject(UUID tenantId, UUID projectId, ProjectUpdate projectUpdate) {
        // TODO: Implement replace project logic
        Project project = new Project();
        project.setId(projectId);
        // Apply updates from projectUpdate
        if (projectUpdate.getDisplayName() != null)
            project.setDisplayName(projectUpdate.getDisplayName());
        if (projectUpdate.getDescription() != null)
            project.setDescription(projectUpdate.getDescription());
        if (projectUpdate.getOwner() != null) {
            project.setOwner(new EntityReference().id(projectUpdate.getOwner().getId())
                    .name(projectUpdate.getOwner().getName()));
        }
        if (projectUpdate.getResourceLimits() != null)
            project.setResourceLimits(projectUpdate.getResourceLimits());
        project.setStatus(Project.StatusEnum.ACTIVE);
        return ResponseEntity.ok(project);
    }

    @Override
    @RequiresPermission(Permission.PROJECT_EDIT)
    public ResponseEntity<Project> updateProject(UUID tenantId, UUID projectId, ProjectUpdate projectUpdate) {
        // TODO: Implement update project logic
        Project project = new Project();
        project.setId(projectId);
        // Apply partial updates
        if (projectUpdate.getDisplayName() != null)
            project.setDisplayName(projectUpdate.getDisplayName());
        if (projectUpdate.getDescription() != null)
            project.setDescription(projectUpdate.getDescription());
        if (projectUpdate.getOwner() != null) {
            project.setOwner(new EntityReference().id(projectUpdate.getOwner().getId())
                    .name(projectUpdate.getOwner().getName()));
        }
        if (projectUpdate.getResourceLimits() != null)
            project.setResourceLimits(projectUpdate.getResourceLimits());
        project.setStatus(Project.StatusEnum.ACTIVE);
        return ResponseEntity.ok(project);
    }
}
