package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.UmlAiChatRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.UmlAiTextBuildRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.UmlAiChatResponse;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatOutcome;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatService;
import com.umlpilot.umlpilot_api.infrastructure.ai.GroqTranscriptionService;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat del asistente de IA con contexto de diagrama y function calling, más dictado por
 * voz (Groq/Whisper) y construcción del diagrama desde una imagen.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final DiagramService diagramService;
    private final GroqTranscriptionService transcriptionService;

    public AiChatController(AiChatService aiChatService, DiagramService diagramService,
                            GroqTranscriptionService transcriptionService) {
        this.aiChatService = aiChatService;
        this.diagramService = diagramService;
        this.transcriptionService = transcriptionService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@PathVariable String projectId, @PathVariable String diagramId,
                                  @RequestBody(required = false) UmlAiChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje es requerido"));
        }

        if (diagramMissing(projectId, diagramId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Diagrama no encontrado"));
        }

        List<Map<String, String>> history = new ArrayList<>();
        if (request.history() != null) {
            for (UmlAiChatRequest.Turn turn : request.history()) {
                if (turn == null || turn.content() == null) continue;
                Map<String, String> entry = new LinkedHashMap<>();
                entry.put("role", turn.role() == null ? "user" : turn.role());
                entry.put("content", turn.content());
                history.add(entry);
            }
        }

        try {
            AiChatOutcome outcome = aiChatService.chat(projectId, diagramId,
                    SecurityUtils.getCurrentUserId(), request.message(), history);
            return ResponseEntity.ok(new UmlAiChatResponse(outcome.reply(), outcome.actions()));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/diagram-from-text")
    public ResponseEntity<?> diagramFromText(@PathVariable String projectId, @PathVariable String diagramId,
                                             @RequestBody(required = false) UmlAiTextBuildRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La descripción es requerida"));
        }
        if (diagramMissing(projectId, diagramId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Diagrama no encontrado"));
        }
        try {
            AiChatOutcome outcome = aiChatService.diagramFromText(projectId, diagramId,
                    SecurityUtils.getCurrentUserId(), request.prompt());
            return ResponseEntity.ok(new UmlAiChatResponse(outcome.reply(), outcome.actions()));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribe(@PathVariable String projectId, @PathVariable String diagramId,
                                        @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El audio es requerido"));
        }
        if (diagramMissing(projectId, diagramId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Diagrama no encontrado"));
        }
        try {
            String text = transcriptionService.transcribe(file.getBytes(), file.getOriginalFilename());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/diagram-from-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> diagramFromImage(@PathVariable String projectId, @PathVariable String diagramId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "instructions", required = false) String instructions) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen es requerida"));
        }
        if (diagramMissing(projectId, diagramId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Diagrama no encontrado"));
        }
        try {
            AiChatOutcome outcome = aiChatService.diagramFromImage(projectId, diagramId,
                    SecurityUtils.getCurrentUserId(), file.getBytes(), file.getContentType(), instructions);
            return ResponseEntity.ok(new UmlAiChatResponse(outcome.reply(), outcome.actions()));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
