package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.service.DiagramService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.UmlAiChatRequest;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.UmlAiChatResponse;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatOutcome;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatService;
import com.umlpilot.umlpilot_api.infrastructure.ai.GroqTranscriptionService;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiChatControllerTest {

    private final AiChatService aiChatService = mock(AiChatService.class);
    private final DiagramService diagramService = mock(DiagramService.class);
    private final GroqTranscriptionService transcriptionService = mock(GroqTranscriptionService.class);
    private final AiChatController controller = new AiChatController(aiChatService, diagramService, transcriptionService);

    @Test
    void blankMessageReturns400() {
        ResponseEntity<?> res = controller.chat("p1", "d1", new UmlAiChatRequest("   ", null));
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
    }

    @Test
    void missingDiagramReturns404() {
        when(diagramService.getDiagram("p1", "d1")).thenThrow(new DomainException("not found"));
        ResponseEntity<?> res = controller.chat("p1", "d1", new UmlAiChatRequest("hola", null));
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void providerFailureReturns503() {
        when(diagramService.getDiagram("p1", "d1")).thenReturn(null);
        when(aiChatService.chat(any(), any(), any(), any(), any())).thenThrow(new LlmException("agotado", true));

        ResponseEntity<?> res = controller.chat("p1", "d1", new UmlAiChatRequest("hola", null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, res.getStatusCode());
    }

    @Test
    void successReturnsReplyAndActions() {
        when(diagramService.getDiagram("p1", "d1")).thenReturn(null);
        when(aiChatService.chat(eq("p1"), eq("d1"), eq("u1"), eq("crea Cliente"), anyList()))
                .thenReturn(new AiChatOutcome("Listo", List.of(Map.of("action", "create", "entity", "element"))));

        ResponseEntity<?> res;
        try (MockedStatic<SecurityUtils> sec = mockStatic(SecurityUtils.class)) {
            sec.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            res = controller.chat("p1", "d1", new UmlAiChatRequest("crea Cliente", null));
        }

        assertEquals(HttpStatus.OK, res.getStatusCode());
        UmlAiChatResponse body = (UmlAiChatResponse) res.getBody();
        assertNotNull(body);
        assertEquals("Listo", body.reply());
        assertEquals(1, body.actions().size());
    }
}
