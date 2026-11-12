package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.application.service.RelationshipService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/relationships")
public class RelationshipController {
    private final RelationshipService relationshipService;
    private final ProjectAccessService accessService;

    public RelationshipController(RelationshipService relationshipService, ProjectAccessService accessService) {
        this.relationshipService = relationshipService;
        this.accessService = accessService;
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @PathVariable String diagramId, @RequestBody Map<String, String> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(relationshipService.createRelationship(new CreateRelationshipCommand(diagramId, body.get("sourceElementId"), body.get("targetElementId"), body.get("relationshipType"), body.get("associationClassElementId"), body.get("sourceHandle"), body.get("targetHandle"))));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId, @PathVariable String diagramId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(relationshipService.getRelationships(diagramId));
    }

    @PutMapping("/{relationshipId}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String relationshipId, @RequestBody Map<String, String> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { return ResponseEntity.ok(relationshipService.updateRelationship(relationshipId, body.get("name"), body.get("type"), body.get("direction"), body.get("associationClassElementId"), body.get("sourceElementId"), body.get("targetElementId"), body.get("sourceHandle"), body.get("targetHandle"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{relationshipId}/waypoints")
    public ResponseEntity<?> updateWaypoints(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String relationshipId, @RequestBody Map<String, String> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { return ResponseEntity.ok(relationshipService.updateWaypoints(relationshipId, body.get("waypoints"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String relationshipId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { relationshipService.deleteRelationship(relationshipId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/multiplicities/{multiplicityId}")
    public ResponseEntity<?> updateMultiplicity(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String multiplicityId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            Integer min = body.get("min") != null ? ((Number) body.get("min")).intValue() : null;
            Integer max = body.get("max") != null ? ((Number) body.get("max")).intValue() : null;
            return ResponseEntity.ok(relationshipService.updateMultiplicity(multiplicityId, min, max));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{relationshipId}/multiplicities")
    public ResponseEntity<?> listMultiplicities(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String relationshipId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(relationshipService.getMultiplicities(relationshipId));
    }

    private ResponseEntity<?> requireMember(String projectId, String diagramId) {
        try {
            accessService.requireDiagramMember(projectId, diagramId, SecurityUtils.getCurrentUserId());
            return null;
        } catch (DomainException e) {
            return forbidden(e);
        }
    }

    private ResponseEntity<?> requireEditor(String projectId, String diagramId) {
        try {
            accessService.requireDiagramEditor(projectId, diagramId, SecurityUtils.getCurrentUserId());
            return null;
        } catch (DomainException e) {
            return forbidden(e);
        }
    }

    private ResponseEntity<?> forbidden(DomainException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
}
