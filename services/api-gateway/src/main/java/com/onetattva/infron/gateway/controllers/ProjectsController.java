package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.ProjectsApi;
import com.onetattva.infron.api.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProjectsController implements ProjectsApi {

    @Override
    public ResponseEntity<ProjectMember> addProjectMember(UUID projectId, ProjectMember projectMember) {
        // TODO: Implement add project member logic
        ProjectMember newMember = new ProjectMember();
        newMember.setProjectId(projectId);
        newMember.setUserId(projectMember.getUserId());
        newMember.setRole(projectMember.getRole());
        return ResponseEntity.status(201).body(newMember);
    }

    @Override
    public ResponseEntity<Project> createProject(Project project) {
        // TODO: Implement project creation logic
        project.setId(UUID.randomUUID());
        project.setStatus("active");
        return ResponseEntity.status(201).body(project);
    }

    @Override
    public ResponseEntity<Void> deleteProject(UUID projectId) {
        // TODO: Implement project deletion logic
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> deleteProjectDatacenterGrant(UUID projectId, String datacenter) {
        // TODO: Implement delete project datacenter grant logic
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Project> getProject(UUID projectId) {
        // TODO: Implement get project details logic
        Project project = new Project();
        project.setId(projectId);
        project.setName("Dummy Project");
        project.setStatus("active");
        return ResponseEntity.ok(project);
    }

    @Override
    public ResponseEntity<GetProjectDatacenterGrant200Response> getProjectDatacenterGrant(UUID projectId, String datacenter) {
        // TODO: Implement get project datacenter grant logic
        GetProjectDatacenterGrant200Response response = new GetProjectDatacenterGrant200Response();
        // Assuming some grant details
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ListProjectMembers200Response> listProjectMembers(UUID projectId) {
        // TODO: Implement list project members logic
        ListProjectMembers200Response response = new ListProjectMembers200Response();
        response.setTotal(1);
        // Add dummy members
        ProjectMember member = new ProjectMember();
        member.setUserId(UUID.randomUUID());
        member.setRole("admin");
        response.setItems(List.of(member));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ProjectList> listProjects() {
        // TODO: Implement list projects logic
        ProjectList projectList = new ProjectList();
        projectList.setTotal(1);
        Project project = new Project();
        project.setId(UUID.randomUUID());
        project.setName("Dummy Project");
        project.setStatus("active");
        projectList.setItems(List.of(project));
        return ResponseEntity.ok(projectList);
    }

    @Override
    public ResponseEntity<Void> removeProjectMember(UUID projectId, UUID userId) {
        // TODO: Implement remove project member logic
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Project> replaceProject(UUID projectId, Project project) {
        // TODO: Implement replace project logic
        project.setId(projectId);
        project.setStatus("active");
        return ResponseEntity.ok(project);
    }

    @Override
    public ResponseEntity<GetProjectDatacenterGrant200Response> replaceProjectDatacenterGrant(UUID projectId, String datacenter, GetProjectDatacenterGrant200Response getProjectDatacenterGrant200Response) {
        // TODO: Implement replace project datacenter grant logic
        return ResponseEntity.ok(getProjectDatacenterGrant200Response);
    }

    @Override
    public ResponseEntity<Project> updateProject(UUID projectId, Project project) {
        // TODO: Implement update project logic
        project.setId(projectId);
        project.setStatus("active");
        return ResponseEntity.ok(project);
    }
}
