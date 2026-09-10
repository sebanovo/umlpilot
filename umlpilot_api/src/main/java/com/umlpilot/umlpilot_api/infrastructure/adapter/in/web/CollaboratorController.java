package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
import com.umlpilot.umlpilot_api.application.service.CollaboratorService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.InviteCollaboratorRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.CollaboratorResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/collaborators")
public class CollaboratorController {
    private final CollaboratorService collaboratorService;
    public CollaboratorController(CollaboratorService collaboratorService) { this.collaboratorService = collaboratorService; }

    @PostMapping
    public ResponseEntity<?> invite(@PathVariable String projectId, @Valid @RequestBody InviteCollaboratorRequest request) {
        try {
            var result = collaboratorService.inviteCollaborator(new InviteCollaboratorCommand(projectId, request.email(), request.role(), SecurityUtils.getCurrentUserId()));
            return ResponseEntity.status(HttpStatus.CREATED).body(new CollaboratorResponse(result.userId(), result.email(), result.firstName(), result.lastName(), result.role(), result.invitationStatus()));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId) {
        try { return ResponseEntity.ok(collaboratorService.getCollaborators(projectId, SecurityUtils.getCurrentUserId()).stream().map(c -> new CollaboratorResponse(c.userId(), c.email(), c.firstName(), c.lastName(), c.role(), c.invitationStatus())).toList()); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/accept")
    public ResponseEntity<?> accept(@PathVariable String projectId) {
        try { collaboratorService.acceptInvitation(projectId, SecurityUtils.getCurrentUserId()); return ResponseEntity.ok(Map.of("message", "Invitación aceptada")); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/reject")
    public ResponseEntity<?> reject(@PathVariable String projectId) {
        try { collaboratorService.rejectInvitation(projectId, SecurityUtils.getCurrentUserId()); return ResponseEntity.ok(Map.of("message", "Invitación rechazada")); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> changeRole(@PathVariable String projectId, @PathVariable String userId, @RequestBody Map<String, String> body) {
        try { collaboratorService.changeRole(projectId, userId, body.get("role"), SecurityUtils.getCurrentUserId()); return ResponseEntity.ok(Map.of("message", "Rol actualizado")); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> remove(@PathVariable String projectId, @PathVariable String userId) {
        try { collaboratorService.removeCollaborator(projectId, userId, SecurityUtils.getCurrentUserId()); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
