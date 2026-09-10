package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/elements")
public class ElementController {
    private final ElementService elementService;
    public ElementController(ElementService elementService) { this.elementService = elementService; }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @PathVariable String diagramId, @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String type = (String) body.get("elementType");
            Integer x = body.get("positionX") != null ? ((Number) body.get("positionX")).intValue() : 0;
            Integer y = body.get("positionY") != null ? ((Number) body.get("positionY")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.createElement(new CreateElementCommand(diagramId, SecurityUtils.getCurrentUserId(), name, type, x, y)));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String diagramId) { return ResponseEntity.ok(elementService.getElements(diagramId)); }

    @PutMapping("/{elementId}")
    public ResponseEntity<?> update(@PathVariable String diagramId, @PathVariable String elementId, @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String visibility = (String) body.get("visibility");
            Integer x = body.get("positionX") != null ? ((Number) body.get("positionX")).intValue() : null;
            Integer y = body.get("positionY") != null ? ((Number) body.get("positionY")).intValue() : null;
            Integer w = body.get("width") != null ? ((Number) body.get("width")).intValue() : null;
            Integer h = body.get("height") != null ? ((Number) body.get("height")).intValue() : null;
            return ResponseEntity.ok(elementService.updateElement(diagramId, elementId, name, visibility, x, y, w, h));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{elementId}")
    public ResponseEntity<?> delete(@PathVariable String elementId) {
        try { elementService.deleteElement(elementId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/attributes")
    public ResponseEntity<?> addAttribute(@PathVariable String elementId, @RequestBody Map<String, Object> body) {
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addAttribute(elementId, (String) body.get("name"), (String) body.get("dataType"), (String) body.get("visibility"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/attributes")
    public ResponseEntity<?> listAttributes(@PathVariable String elementId) { return ResponseEntity.ok(elementService.getAttributes(elementId)); }

    @PutMapping("/attributes/{attributeId}")
    public ResponseEntity<?> updateAttribute(@PathVariable String attributeId, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(elementService.updateAttribute(attributeId, body.get("name"), body.get("dataType"), body.get("visibility"), body.get("defaultValue"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/attributes/{attributeId}")
    public ResponseEntity<?> deleteAttribute(@PathVariable String attributeId) {
        try { elementService.deleteAttribute(attributeId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/methods")
    public ResponseEntity<?> addMethod(@PathVariable String elementId, @RequestBody Map<String, Object> body) {
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addMethod(elementId, (String) body.get("name"), (String) body.get("returnType"), (String) body.get("visibility"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/methods")
    public ResponseEntity<?> listMethods(@PathVariable String elementId) { return ResponseEntity.ok(elementService.getMethods(elementId)); }

    @PutMapping("/methods/{methodId}")
    public ResponseEntity<?> updateMethod(@PathVariable String methodId, @RequestBody Map<String, String> body) {
        try { return ResponseEntity.ok(elementService.updateMethod(methodId, body.get("name"), body.get("returnType"), body.get("visibility"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/methods/{methodId}")
    public ResponseEntity<?> deleteMethod(@PathVariable String methodId) {
        try { elementService.deleteMethod(methodId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/methods/{methodId}/parameters")
    public ResponseEntity<?> addParameter(@PathVariable String methodId, @RequestBody Map<String, Object> body) {
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addParameter(methodId, (String) body.get("name"), (String) body.get("dataType"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/parameters/{parameterId}")
    public ResponseEntity<?> deleteParameter(@PathVariable String parameterId) {
        try { elementService.deleteParameter(parameterId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/literals")
    public ResponseEntity<?> addLiteral(@PathVariable String elementId, @RequestBody Map<String, Object> body) {
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addLiteral(elementId, (String) body.get("name"), (String) body.get("value"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/literals")
    public ResponseEntity<?> listLiterals(@PathVariable String elementId) { return ResponseEntity.ok(elementService.getLiterals(elementId)); }

    @DeleteMapping("/literals/{literalId}")
    public ResponseEntity<?> deleteLiteral(@PathVariable String literalId) {
        try { elementService.deleteLiteral(literalId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
