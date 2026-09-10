package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.*;
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
    public RelationshipController(RelationshipService relationshipService) { this.relationshipService = relationshipService; }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String diagramId, @RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(relationshipService.createRelationship(new CreateRelationshipCommand(diagramId, body.get("sourceElementId"), body.get("targetElementId"), body.get("relationshipType"))));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String diagramId) { return ResponseEntity.ok(relationshipService.getRelationships(diagramId)); }

    @PutMapping("/{relationshipId}")
    public ResponseEntity<?> update(@PathVariable String relationshipId, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(relationshipService.updateRelationship(relationshipId, body.get("name"), body.get("type"), body.get("direction"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<?> delete(@PathVariable String relationshipId) {
        try { relationshipService.deleteRelationship(relationshipId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/multiplicities/{multiplicityId}")
    public ResponseEntity<?> updateMultiplicity(@PathVariable String multiplicityId, @RequestBody Map<String, Object> body) {
        try {
            Integer min = body.get("min") != null ? ((Number) body.get("min")).intValue() : null;
            Integer max = body.get("max") != null ? ((Number) body.get("max")).intValue() : null;
            return ResponseEntity.ok(relationshipService.updateMultiplicity(multiplicityId, min, max));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{relationshipId}/multiplicities")
    public ResponseEntity<?> listMultiplicities(@PathVariable String relationshipId) { return ResponseEntity.ok(relationshipService.getMultiplicities(relationshipId)); }
}
