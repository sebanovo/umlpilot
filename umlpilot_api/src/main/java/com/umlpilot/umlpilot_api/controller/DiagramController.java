package com.umlpilot.umlpilot_api.controller;

import com.umlpilot.umlpilot_api.dto.CreateDiagramRequest;
import com.umlpilot.umlpilot_api.dto.DiagramResponse;
import com.umlpilot.umlpilot_api.model.User;
import com.umlpilot.umlpilot_api.service.DiagramService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams")
public class DiagramController {

    private final DiagramService diagramService;

    public DiagramController(DiagramService diagramService) {
        this.diagramService = diagramService;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @Valid @RequestBody CreateDiagramRequest request, @AuthenticationPrincipal User user) {
        try {
            DiagramResponse response = diagramService.createDiagram(projectId, request, user.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId, @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(diagramService.getDiagrams(projectId, user.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            DiagramResponse response = diagramService.getDiagram(projectId, id, user.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String id, @RequestBody Map<String, String> body, @AuthenticationPrincipal User user) {
        try {
            DiagramResponse response = diagramService.updateDiagram(projectId, id, body.get("name"), body.get("description"), body.get("canvasData"), user.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<?> lock(@PathVariable String projectId, @PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            DiagramResponse response = diagramService.lockDiagram(projectId, id, user.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlock(@PathVariable String projectId, @PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            DiagramResponse response = diagramService.unlockDiagram(projectId, id, user.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String id, @AuthenticationPrincipal User user) {
        try {
            diagramService.deleteDiagram(projectId, id, user.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
