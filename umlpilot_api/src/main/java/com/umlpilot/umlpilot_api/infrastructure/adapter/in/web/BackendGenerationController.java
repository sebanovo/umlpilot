package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.application.service.SpringBootGenerationService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;

/**
 * Adaptador de entrada: genera un backend Spring Boot (.zip) a partir de un diagrama.
 *
 * <p>El método devuelve {@link StreamingResponseBody} directamente (no envuelto en
 * {@code ResponseEntity}) porque Spring MVC solo resuelve el streaming cuando el tipo de
 * retorno es el propio cuerpo; los errores se resuelven con {@code @ExceptionHandler}.</p>
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/generate")
public class BackendGenerationController {

    private static final String FILE_NAME = "generated-backend.zip";
    private static final String ZIP_CONTENT_TYPE = "application/zip";

    private final SpringBootGenerationService generationService;
    private final DiagramService diagramService;

    public BackendGenerationController(SpringBootGenerationService generationService,
                                       DiagramService diagramService) {
        this.generationService = generationService;
        this.diagramService = diagramService;
    }

    @GetMapping("/backend")
    public StreamingResponseBody generateBackend(HttpServletResponse response,
                                                 @PathVariable String projectId,
                                                 @PathVariable String diagramId) {
        if (diagramMissing(projectId, diagramId)) {
            throw new DiagramNotFoundException("Diagrama no encontrado");
        }

        Map<String, String> files = generationService.generateFiles(diagramId);

        response.setContentType(ZIP_CONTENT_TYPE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + FILE_NAME + "\"");
        return outputStream -> generationService.writeZip(files, outputStream);
    }

    @ExceptionHandler(DiagramNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleDiagramNotFound(DiagramNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<Map<String, String>> handleDomainException(DomainException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    private boolean diagramMissing(String projectId, String diagramId) {
        try {
            diagramService.getDiagram(projectId, diagramId);
            return false;
        } catch (DomainException e) {
            return true;
        }
    }
}
