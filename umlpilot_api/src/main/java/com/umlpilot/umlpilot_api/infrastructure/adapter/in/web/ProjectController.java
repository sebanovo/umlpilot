package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.CreateProjectCommand;
import com.umlpilot.umlpilot_api.application.service.ProjectService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.CreateProjectRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.CollaboratorResponse;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.ProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<?> create(@Valid @RequestBody CreateProjectRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            var result = projectService.createProject(new CreateProjectCommand(request.name(), request.description(), userId));
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserDetails userDetails) {
        String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
        return ResponseEntity.ok(projectService.getUserProjects(userId).stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            return ResponseEntity.ok(toResponse(projectService.getProject(id, userId)));
        } catch (DomainException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            return ResponseEntity.ok(toResponse(projectService.updateProject(id, body.get("name"), body.get("description"), userId)));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            projectService.deleteProject(id, userId);
            return ResponseEntity.noContent().build();
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<?> getCollaborators(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            return ResponseEntity.ok(projectService.getCollaborators(id, userId).stream()
                    .map(c -> new CollaboratorResponse(c.userId(), c.email(), c.firstName(), c.lastName(), c.role(), c.invitationStatus()))
                    .toList());
        } catch (DomainException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private ProjectResponse toResponse(com.umlpilot.umlpilot_api.application.dto.ProjectResult result) {
        return new ProjectResponse(result.id(), result.name(), result.description(), result.creatorId(), result.status(), result.createdAt(), result.updatedAt());
    }
}
