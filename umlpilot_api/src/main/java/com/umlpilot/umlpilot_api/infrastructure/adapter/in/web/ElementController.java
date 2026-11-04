package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/elements")
public class ElementController {
    private final ElementService elementService;
    private final DiagramEventPublisher eventPublisher;
    private final ProjectAccessService accessService;

    public ElementController(ElementService elementService, DiagramEventPublisher eventPublisher,
                             ProjectAccessService accessService) {
        this.elementService = elementService;
        this.eventPublisher = eventPublisher;
        this.accessService = accessService;
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll(@PathVariable String projectId, @PathVariable String diagramId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            elementService.deleteAllElements(diagramId);
            eventPublisher.diagramChanged(diagramId, SecurityUtils.getCurrentUserId(), "delete_all");
            return ResponseEntity.noContent().build();
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String projectId, @PathVariable String diagramId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            String name = (String) body.get("name");
            String type = (String) body.get("elementType");
            Integer x = body.get("positionX") != null ? ((Number) body.get("positionX")).intValue() : 0;
            Integer y = body.get("positionY") != null ? ((Number) body.get("positionY")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.createElement(new CreateElementCommand(diagramId, SecurityUtils.getCurrentUserId(), name, type, x, y)));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String projectId, @PathVariable String diagramId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(elementService.getElements(diagramId));
    }

    @PutMapping("/{elementId}")
    public ResponseEntity<?> update(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
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
    public ResponseEntity<?> delete(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { elementService.deleteElement(elementId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/attributes")
    public ResponseEntity<?> addAttribute(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addAttribute(elementId, (String) body.get("name"), (String) body.get("dataType"), (String) body.get("visibility"), (String) body.get("alias"), (String) body.get("stereotype"), (String) body.get("defaultValue"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/attributes")
    public ResponseEntity<?> listAttributes(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(elementService.getAttributes(elementId));
    }

    @PutMapping("/attributes/{attributeId}")
    public ResponseEntity<?> updateAttribute(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String attributeId, @RequestBody Map<String, String> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { return ResponseEntity.ok(elementService.updateAttribute(attributeId, body.get("name"), body.get("dataType"), body.get("visibility"), body.get("alias"), body.get("stereotype"), body.get("defaultValue"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/attributes/{attributeId}")
    public ResponseEntity<?> deleteAttribute(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String attributeId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { elementService.deleteAttribute(attributeId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/methods")
    public ResponseEntity<?> addMethod(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addMethod(elementId, (String) body.get("name"), (String) body.get("returnType"), (String) body.get("visibility"), (String) body.get("alias"), (String) body.get("stereotype"), (String) body.get("defaultValue"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/methods")
    public ResponseEntity<?> listMethods(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(elementService.getMethods(elementId));
    }

    @PutMapping("/methods/{methodId}")
    public ResponseEntity<?> updateMethod(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String methodId, @RequestBody Map<String, String> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { return ResponseEntity.ok(elementService.updateMethod(methodId, body.get("name"), body.get("returnType"), body.get("visibility"), body.get("alias"), body.get("stereotype"), body.get("defaultValue"))); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/methods/{methodId}")
    public ResponseEntity<?> deleteMethod(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String methodId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { elementService.deleteMethod(methodId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/methods/{methodId}/parameters")
    public ResponseEntity<?> addParameter(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String methodId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addParameter(methodId, (String) body.get("name"), (String) body.get("dataType"), (String) body.get("direction"), (String) body.get("notes"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/parameters/{parameterId}")
    public ResponseEntity<?> deleteParameter(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String parameterId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { elementService.deleteParameter(parameterId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{elementId}/literals")
    public ResponseEntity<?> addLiteral(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId, @RequestBody Map<String, Object> body) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try {
            Integer order = body.get("orderIndex") != null ? ((Number) body.get("orderIndex")).intValue() : 0;
            return ResponseEntity.status(HttpStatus.CREATED).body(elementService.addLiteral(elementId, (String) body.get("name"), (String) body.get("value"), order));
        } catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/{elementId}/literals")
    public ResponseEntity<?> listLiterals(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String elementId) {
        ResponseEntity<?> denied = requireMember(projectId, diagramId);
        if (denied != null) return denied;
        return ResponseEntity.ok(elementService.getLiterals(elementId));
    }

    @DeleteMapping("/literals/{literalId}")
    public ResponseEntity<?> deleteLiteral(@PathVariable String projectId, @PathVariable String diagramId, @PathVariable String literalId) {
        ResponseEntity<?> denied = requireEditor(projectId, diagramId);
        if (denied != null) return denied;
        try { elementService.deleteLiteral(literalId); return ResponseEntity.noContent().build(); }
        catch (DomainException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
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
