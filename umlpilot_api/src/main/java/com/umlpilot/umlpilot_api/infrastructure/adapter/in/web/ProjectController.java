package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.CreateProjectCommand;
import com.umlpilot.umlpilot_api.application.service.ProjectService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.CreateProjectRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;
    public ProjectController(ProjectService projectService) { this.projectService = projectService; }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateProjectRequest request) {
        try {
            String userId = SecurityUtils.getCurrentUserId();
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(projectService.createProject(new CreateProjectCommand(request.name(), request.description(), userId))));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list() { return ResponseEntity.ok(projectService.getUserProjects(SecurityUtils.getCurrentUserId()).stream().map(this::toResponse).toList()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        try { return ResponseEntity.ok(toResponse(projectService.getProject(id, SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(toResponse(projectService.updateProject(id, body.get("name"), body.get("description"), SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try { projectService.deleteProject(id, SecurityUtils.getCurrentUserId()); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private ProjectResponse toResponse(com.umlpilot.umlpilot_api.application.dto.ProjectResult result) {
        return new ProjectResponse(result.id(), result.name(), result.description(), result.creatorId(), result.status(), result.createdAt(), result.updatedAt());
    }
}
