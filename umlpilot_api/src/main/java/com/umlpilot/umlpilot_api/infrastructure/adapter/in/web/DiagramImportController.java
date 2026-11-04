package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.ImportXmiResult;
import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.XmiImportService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Adaptador de entrada: importa un archivo XMI de clases y lo añade al diagrama actual.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/import")
public class DiagramImportController {

    private static final long MAX_FILE_SIZE_MB = 10;

    private final XmiImportService importService;
    private final DiagramService diagramService;

    public DiagramImportController(XmiImportService importService, DiagramService diagramService) {
        this.importService = importService;
        this.diagramService = diagramService;
    }

    @PostMapping(value = "/xmi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importXmi(@PathVariable String projectId, @PathVariable String diagramId,
                                       @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo XMI es requerido"));
        }
        if (diagramMissing(projectId, diagramId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Diagrama no encontrado"));
        }
        try {
            ImportXmiResult result = importService.importDiagram(
                    diagramId, SecurityUtils.getCurrentUserId(), file.getBytes());
            return ResponseEntity.ok(result);
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se pudo leer el archivo XMI"));
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("error", "El archivo supera el límite de " + MAX_FILE_SIZE_MB + " MB"));
    }

    private boolean diagramMissing(String projectId, String diagramId) {
        try {
            // Importar modifica el diagrama: se exige rol de edición.
            diagramService.requireEditor(projectId, diagramId, SecurityUtils.getCurrentUserId());
            return false;
        } catch (DomainException e) {
            return true;
        }
    }
}
