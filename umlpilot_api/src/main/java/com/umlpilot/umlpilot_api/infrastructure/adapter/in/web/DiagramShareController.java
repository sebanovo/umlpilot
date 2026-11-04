package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramShareService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Gestión del enlace público de un diagrama (solo owner/editor del proyecto). */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/share")
public class DiagramShareController {

    private final DiagramShareService shareService;

    public DiagramShareController(DiagramShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping
    public ResponseEntity<?> status(@PathVariable String projectId, @PathVariable String diagramId) {
        try {
            return ResponseEntity.ok(shareService.getStatus(projectId, diagramId, SecurityUtils.getCurrentUserId()));
        } catch (DomainException e) {
            return forbidden(e);
        }
    }

    @PostMapping
    public ResponseEntity<?> enable(@PathVariable String projectId, @PathVariable String diagramId,
                                    @RequestBody(required = false) Map<String, String> body) {
        try {
            String role = body != null ? body.get("role") : null;
            return ResponseEntity.ok(shareService.enable(projectId, diagramId, SecurityUtils.getCurrentUserId(), role));
        } catch (DomainException e) {
            return forbidden(e);
        }
    }

    @DeleteMapping
    public ResponseEntity<?> disable(@PathVariable String projectId, @PathVariable String diagramId) {
        try {
            shareService.disable(projectId, diagramId, SecurityUtils.getCurrentUserId());
            return ResponseEntity.noContent().build();
        } catch (DomainException e) {
            return forbidden(e);
        }
    }

    private ResponseEntity<?> forbidden(DomainException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
    }
}
