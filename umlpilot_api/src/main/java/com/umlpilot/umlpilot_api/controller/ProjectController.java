package com.umlpilot.umlpilot_api.controller;

import com.umlpilot.umlpilot_api.dto.CollaboratorResponse;
import com.umlpilot.umlpilot_api.dto.CreateProjectRequest;
import com.umlpilot.umlpilot_api.dto.ProjectResponse;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal User user) {
        try {
            ProjectResponse response = projectService.createProject(request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.getUserProjects(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            ProjectResponse project = projectService.getProject(id, user.getId());
            return ResponseEntity.ok(project);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            ProjectResponse response = projectService.updateProject(id, body.get("name"), body.get("description"), user.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            projectService.deleteProject(id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<?> getCollaborators(@PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(projectService.getCollaborators(id, user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
