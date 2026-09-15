package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.UmlAiChatRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.UmlAiChatResponse;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatOutcome;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatService;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chat del asistente de IA con contexto de diagrama y function calling.
 */
@RestController
@RequestMapping("/api/v1/projects/{projectId}/diagrams/{diagramId}/ai")
public class AiChatController {

    private final AiChatService aiChatService;
    private final DiagramService diagramService;

    public AiChatController(AiChatService aiChatService, DiagramService diagramService) {
        this.aiChatService = aiChatService;
        this.diagramService = diagramService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@PathVariable String projectId, @PathVariable String diagramId,
                                  @RequestBody(required = false) UmlAiChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje es requerido"));
        }

        try {
            diagramService.getDiagram(projectId, diagramId);
        } catch (DomainException e) {
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
}
