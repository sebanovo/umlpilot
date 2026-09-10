package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.CreateDiagramCommand;
import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.CreateDiagramRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.DiagramResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams")
public class DiagramController {
    private final DiagramService diagramService;
    public DiagramController(DiagramService diagramService) { this.diagramService = diagramService; }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @Valid @RequestBody CreateDiagramRequest request) {
        try {             return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(diagramService.createDiagram(new CreateDiagramCommand(projectId, request.name(), request.type(), request.description()), SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId) {
        try { return ResponseEntity.ok(diagramService.getDiagrams(projectId, SecurityUtils.getCurrentUserId()).stream().map(this::toResponse).toList()); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String projectId, @PathVariable String id) {
        try { return ResponseEntity.ok(toResponse(diagramService.getDiagram(projectId, id, SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String id, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(toResponse(diagramService.updateDiagram(projectId, id, body.get("name"), body.get("description"), body.get("canvasData"), SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<?> lock(@PathVariable String projectId, @PathVariable String id) {
        try { return ResponseEntity.ok(toResponse(diagramService.lockDiagram(projectId, id, SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<?> unlock(@PathVariable String projectId, @PathVariable String id) {
        try { return ResponseEntity.ok(toResponse(diagramService.unlockDiagram(projectId, id, SecurityUtils.getCurrentUserId()))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String id) {
        try { diagramService.deleteDiagram(projectId, id, SecurityUtils.getCurrentUserId()); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    private DiagramResponse toResponse(com.umlpilot.umlpilot_api.application.dto.DiagramResult result) {
        return new DiagramResponse(result.id(), result.projectId(), result.name(), result.type(), result.description(), result.canvasData(), result.version(), result.isLocked(), result.lockedByUserId(), result.createdAt(), result.updatedAt());
    }
}
