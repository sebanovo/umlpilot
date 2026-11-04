package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramShareService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Acceso público (sin autenticación) a un diagrama compartido por enlace. */
@RestController
@RequestMapping("/api/v1/public/diagrams")
public class PublicDiagramController {

    private final DiagramShareService shareService;

    public PublicDiagramController(DiagramShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> get(@PathVariable String token) {
        try {
            return ResponseEntity.ok(shareService.getPublicView(token));
        } catch (DomainException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    /** Requiere autenticación: une al usuario al proyecto con el rol del enlace. */
    @PostMapping("/{token}/join")
    public ResponseEntity<?> join(@PathVariable String token) {
        try {
            return ResponseEntity.ok(shareService.join(token, SecurityUtils.getCurrentUserId()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
