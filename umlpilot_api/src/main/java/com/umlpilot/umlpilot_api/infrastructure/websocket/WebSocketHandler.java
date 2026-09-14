package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.infrastructure.security.JwtService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final JwtService jwtService;
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Set<String>> diagramRooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public WebSocketHandler(JwtService jwtService, SimpMessagingTemplate messagingTemplate) {
        this.jwtService = jwtService;
        this.messagingTemplate = messagingTemplate;
    }

    private String extractUserId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null) {
            return principal.getName();
        }
        String token = headerAccessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtService.isTokenValid(token)) {
                return jwtService.extractEmail(token);
            }
        }
        return null;
    }

    @MessageMapping("/joinDiagram")
    public void joinDiagram(@Payload JoinDiagramMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null) return;

        String diagramId = message.diagramId();
        diagramRooms.computeIfAbsent(diagramId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        sessionUserMap.put(userId, diagramId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("users", new ArrayList<>(diagramRooms.get(diagramId)));
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", (Object) response);
    }

    @MessageMapping("/leaveDiagram")
    public void leaveDiagram(@Payload LeaveDiagramMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null) return;

        String diagramId = message.diagramId();
        removeFromRoom(userId, diagramId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", (Object) response);
    }

    @MessageMapping("/diagram/update")
    public void diagramUpdate(@Payload DiagramUpdateMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null) return;

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("type", message.type());
        response.put("data", message.data());
        messagingTemplate.convertAndSend("/topic/diagram/" + message.diagramId() + "/changes", (Object) response);
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sessionUserMap.remove(sessionId);
        if (userId != null) {
            String diagramId = findDiagramForUser(userId);
            if (diagramId != null) {
                removeFromRoom(userId, diagramId);
            }
        }
    }

    private void removeFromRoom(String userId, String diagramId) {
        Set<String> users = diagramRooms.get(diagramId);
        if (users != null) {
            users.remove(userId);
            if (users.isEmpty()) {
                diagramRooms.remove(diagramId);
            }
        }
    }

    private String findDiagramForUser(String userId) {
        for (Map.Entry<String, Set<String>> entry : diagramRooms.entrySet()) {
            if (entry.getValue().contains(userId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record JoinDiagramMessage(String diagramId) {}
    public record LeaveDiagramMessage(String diagramId) {}
    public record DiagramUpdateMessage(String diagramId, String type, Object data) {}
}
