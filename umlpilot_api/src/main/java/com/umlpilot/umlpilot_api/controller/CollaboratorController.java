package com.umlpilot.umlpilot_api.controller;

import com.umlpilot.umlpilot_api.dto.CollaboratorResponse;
import com.umlpilot.umlpilot_api.dto.InviteCollaboratorRequest;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.service.CollaboratorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<?> invite(@PathVariable String projectId, @Valid @RequestBody InviteCollaboratorRequest request, @AuthenticationPrincipal User user) {
        try {
            CollaboratorResponse response = collaboratorService.inviteCollaborator(projectId, request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(collaboratorService.getCollaborators(projectId, user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/accept")
    public ResponseEntity<?> accept(@PathVariable String projectId, @AuthenticationPrincipal User user) {
        try {
            collaboratorService.acceptInvitation(projectId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Invitación aceptada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/reject")
    public ResponseEntity<?> reject(@PathVariable String projectId, @AuthenticationPrincipal User user) {
        try {
            collaboratorService.rejectInvitation(projectId, user.getId());
            return ResponseEntity.ok(Map.of("message", "Invitación rechazada"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> changeRole(@PathVariable String projectId, @PathVariable String userId, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            collaboratorService.changeRole(projectId, userId, body.get("role"), user.getId());
            return ResponseEntity.ok(Map.of("message", "Rol actualizado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> remove(@PathVariable String projectId, @PathVariable String userId, @AuthenticationPrincipal User user) {
        try {
            collaboratorService.removeCollaborator(projectId, userId, user.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
