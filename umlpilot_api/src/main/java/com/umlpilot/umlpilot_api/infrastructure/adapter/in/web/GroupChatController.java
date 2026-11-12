package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.ChatImageData;
import com.umlpilot.umlpilot_api.application.dto.ChatSendResult;
import com.umlpilot.umlpilot_api.application.dto.SendChatMessageCommand;
import com.umlpilot.umlpilot_api.application.service.GroupChatService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.SendChatMessageRequest;
import com.umlpilot.umlpilot_api.infrastructure.ai.GroqTranscriptionService;
import com.umlpilot.umlpilot_api.infrastructure.ai.LlmException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat")
public class GroupChatController {

    private final GroupChatService chatService;
    private final GroqTranscriptionService transcriptionService;

    public GroupChatController(GroupChatService chatService,
                               GroqTranscriptionService transcriptionService) {
        this.chatService = chatService;
        this.transcriptionService = transcriptionService;
    }

    @GetMapping
    public ResponseEntity<?> conversation(@PathVariable String projectId) {
        try {
            return ResponseEntity.ok(chatService.getOrCreateConversation(
                    projectId, SecurityUtils.getCurrentUserId()));
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        }
    }

    @GetMapping("/messages")
    public ResponseEntity<?> messages(@PathVariable String projectId,
                                      @RequestParam(required = false) String before,
                                      @RequestParam(defaultValue = "50") int limit) {
        try {
            return ResponseEntity.ok(chatService.listMessages(
                    projectId, SecurityUtils.getCurrentUserId(), before, limit));
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        }
    }

    @PostMapping("/messages")
    public ResponseEntity<?> send(@PathVariable String projectId,
                                  @RequestBody(required = false) SendChatMessageRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "El mensaje es requerido"));
        }
        try {
            ChatSendResult result = chatService.sendMessage(new SendChatMessageCommand(
                    projectId,
                    SecurityUtils.getCurrentUserId(),
                    request.clientMessageId(),
                    request.content(),
                    request.diagramId()));
            return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                    .body(result);
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        }
    }

    @PostMapping(value = "/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendImage(@PathVariable String projectId,
                                       @RequestParam("file") MultipartFile file,
                                       @RequestParam(value = "diagramId", required = false) String diagramId,
                                       @RequestParam(value = "instructions", required = false) String instructions,
                                       @RequestParam(value = "clientMessageId", required = false) String clientMessageId) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "La imagen es requerida"));
        }
        try {
            ChatSendResult result = chatService.sendImageMessage(
                    projectId,
                    SecurityUtils.getCurrentUserId(),
                    clientMessageId,
                    diagramId,
                    instructions,
                    file.getOriginalFilename(),
                    file.getBytes(),
                    file.getContentType());
            return ResponseEntity.status(result.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED)
                    .body(result);
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se pudo procesar la imagen"));
        }
    }

    @GetMapping("/messages/{messageId}/image")
    public ResponseEntity<?> image(@PathVariable String projectId,
                                   @PathVariable String messageId) {
        try {
            ChatImageData image = chatService.getImage(
                    projectId, messageId, SecurityUtils.getCurrentUserId());
            MediaType type;
            try {
                type = MediaType.parseMediaType(image.contentType());
            } catch (RuntimeException invalidType) {
                type = MediaType.APPLICATION_OCTET_STREAM;
            }
            return ResponseEntity.ok().contentType(type).body(image.bytes());
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        }
    }

    @PostMapping("/messages/{messageId}/retry")
    public ResponseEntity<?> retry(@PathVariable String projectId,
                                   @PathVariable String messageId) {
        try {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(
                    chatService.retryMessage(projectId, messageId, SecurityUtils.getCurrentUserId()));
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        }
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> transcribe(@PathVariable String projectId,
                                        @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El audio es requerido"));
        }
        try {
            chatService.requireMember(projectId, SecurityUtils.getCurrentUserId());
            String text = transcriptionService.transcribe(file.getBytes(), file.getOriginalFilename());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (LlmException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", e.getMessage()));
        } catch (DomainException e) {
            return error(e);
        } catch (DataAccessException e) {
            return unavailable(e);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se pudo transcribir el audio"));
        }
    }

    private ResponseEntity<?> error(DomainException e) {
        String message = e.getMessage() == null ? "Solicitud inválida" : e.getMessage();
        if (message.toLowerCase().contains("no encontrado")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", message));
        }
        if (message.toLowerCase().contains("acceso")
                || message.toLowerCase().contains("permisos")
                || message.toLowerCase().contains("owner")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", message));
        }
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private ResponseEntity<?> unavailable(DataAccessException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "El almacén del chat no está disponible"));
    }
}
