package com.onetattva.infron.gateway.controllers;

import com.onetattva.infron.api.ProjectsApi;
import com.onetattva.infron.api.model.Project;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class ProjectsController implements ProjectsApi {

    @Override
    public ResponseEntity<List<Project>> apiV1ProjectsGet() {

        Project test = new Project();
        test.setName("First Project");
        test.setId(UUID.randomUUID().toString());
        List<Project> projects = List.of(test);
        return new ResponseEntity<>(projects, HttpStatus.OK);
    }
}
