package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.AiChatRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.AiChatResponse;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiOrchestrator;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmException;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmMessage;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiOrchestrator orchestrator;

    public AiController(AiOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El prompt es requerido"));
        }

        List<LlmMessage> messages = new ArrayList<>();
        if (request.system() != null && !request.system().isBlank()) {
            messages.add(LlmMessage.system(request.system()));
        }
        messages.add(LlmMessage.user(request.prompt()));

        try {
            LlmResponse response = orchestrator.complete(messages);
            return ResponseEntity.ok(new AiChatResponse(response.provider(), response.model(), response.content()));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(orchestrator.status());
    }
}
