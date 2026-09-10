package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
import com.umlpilot.umlpilot_api.application.service.CollaboratorService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.InviteCollaboratorRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.CollaboratorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/collaborators")
public class CollaboratorController {

    private final CollaboratorService collaboratorService;

    public CollaboratorController(CollaboratorService collaboratorService) {
        this.collaboratorService = collaboratorService;
    }

    @PostMapping
    public ResponseEntity<?> invite(@PathVariable String projectId, @Valid @RequestBody InviteCollaboratorRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            var result = collaboratorService.inviteCollaborator(new InviteCollaboratorCommand(projectId, request.email(), request.role(), userId));
            return ResponseEntity.status(HttpStatus.CREATED).body(new CollaboratorResponse(result.userId(), result.email(), result.firstName(), result.lastName(), result.role(), result.invitationStatus()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            return ResponseEntity.ok(collaboratorService.getCollaborators(projectId, userId).stream()
                    .map(c -> new CollaboratorResponse(c.userId(), c.email(), c.firstName(), c.lastName(), c.role(), c.invitationStatus()))
                    .toList());
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/accept")
    public ResponseEntity<?> accept(@PathVariable String projectId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            collaboratorService.acceptInvitation(projectId, userId);
            return ResponseEntity.ok(Map.of("message", "Invitación aceptada"));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reject")
    public ResponseEntity<?> reject(@PathVariable String projectId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String userId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            collaboratorService.rejectInvitation(projectId, userId);
            return ResponseEntity.ok(Map.of("message", "Invitación rechazada"));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> changeRole(@PathVariable String projectId, @PathVariable String userId, @RequestBody Map<String, String> body, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String requesterId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            collaboratorService.changeRole(projectId, userId, body.get("role"), requesterId);
            return ResponseEntity.ok(Map.of("message", "Rol actualizado"));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> remove(@PathVariable String projectId, @PathVariable String userId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            String requesterId = ((com.umlpilot.umlpilot_api.domain.model.User) userDetails).getId().value();
            collaboratorService.removeCollaborator(projectId, userId, requesterId);
            return ResponseEntity.noContent().build();
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
