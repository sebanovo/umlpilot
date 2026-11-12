package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.XmiExportService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Adaptador de entrada: descarga un diagrama como XMI compatible con Enterprise Architect. */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/export")
public class XmiExportController {

    private static final MediaType XMI_MEDIA_TYPE = MediaType.parseMediaType("application/xml;charset=windows-1252");

    private final XmiExportService exportService;
    private final DiagramService diagramService;

    public XmiExportController(XmiExportService exportService, DiagramService diagramService) {
        this.exportService = exportService;
        this.diagramService = diagramService;
    }

    @GetMapping("/xmi")
    public ResponseEntity<byte[]> exportXmi(@PathVariable String projectId, @PathVariable String diagramId) {
        diagramService.getDiagram(projectId, diagramId, SecurityUtils.getCurrentUserId());
        byte[] xml = exportService.export(diagramId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(XMI_MEDIA_TYPE);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"diagram_" + diagramId + ".xml\"");
        return new ResponseEntity<>(xml, headers, HttpStatus.OK);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
