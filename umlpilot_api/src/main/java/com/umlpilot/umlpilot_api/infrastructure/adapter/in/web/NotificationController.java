package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.NotificationResult;
import com.umlpilot.umlpilot_api.application.service.NotificationService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.response.NotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        return ResponseEntity.ok(notificationService.getNotifications(SecurityUtils.getCurrentUserId(), unreadOnly)
                .stream().map(this::toResponse).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(SecurityUtils.getCurrentUserId())));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id) {
        try {
            notificationService.markAsRead(id, SecurityUtils.getCurrentUserId());
            return ResponseEntity.noContent().build();
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead() {
        notificationService.markAllAsRead(SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(Map.of("message", "Notificaciones marcadas como leídas"));
    }

    @PostMapping("/{id}/respond")
    public ResponseEntity<?> respond(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            notificationService.respondToInvitation(id, SecurityUtils.getCurrentUserId(), body.get("action"));
            return ResponseEntity.ok(Map.of("message", "Respuesta registrada"));
        } catch (DomainException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private NotificationResponse toResponse(NotificationResult result) {
        return new NotificationResponse(result.id(), result.type(), result.title(), result.message(),
                result.projectId(), result.role(), result.read(), result.readAt(), result.createdAt(),
                result.invitationStatus());
    }
}
