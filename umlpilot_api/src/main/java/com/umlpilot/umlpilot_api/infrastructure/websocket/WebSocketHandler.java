package com.umlpilot.umlpilot_api.infrastructure.websocket;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Set<String>> diagramRooms = new ConcurrentHashMap<>();
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();

    public WebSocketHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/joinDiagram")
    public void joinDiagram(@Payload JoinDiagramMessage message, Principal principal) {
        String userId = principal.getName();
        String diagramId = message.diagramId();
        diagramRooms.computeIfAbsent(diagramId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        userSessions.put(userId, diagramId);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("users", new ArrayList<>(diagramRooms.get(diagramId)));
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", (Object) response);
    }

    @MessageMapping("/leaveDiagram")
    public void leaveDiagram(@Payload LeaveDiagramMessage message, Principal principal) {
        String userId = principal.getName();
        String diagramId = message.diagramId();
        Set<String> users = diagramRooms.get(diagramId);
        if (users != null) { users.remove(userId); if (users.isEmpty()) diagramRooms.remove(diagramId); }
        userSessions.remove(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", (Object) response);
    }

    @MessageMapping("/diagram/update")
    public void diagramUpdate(@Payload DiagramUpdateMessage message, Principal principal) {
        String userId = principal.getName();
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("type", message.type());
        response.put("data", message.data());
        messagingTemplate.convertAndSend("/topic/diagram/" + message.diagramId() + "/changes", (Object) response);
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        userSessions.entrySet().removeIf(entry -> {
            if (entry.getKey().equals(sessionId)) {
                String diagramId = entry.getValue();
                Set<String> users = diagramRooms.get(diagramId);
                if (users != null) { users.remove(sessionId); if (users.isEmpty()) diagramRooms.remove(diagramId); }
                return true;
            }
            return false;
        });
    }

    public record JoinDiagramMessage(String diagramId) {}
    public record LeaveDiagramMessage(String diagramId) {}
    public record DiagramUpdateMessage(String diagramId, String type, Object data) {}
}
