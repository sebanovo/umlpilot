package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import com.umlpilot.umlpilot_api.infrastructure.security.JwtService;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
public class WebSocketHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectAccessService accessService;
    private final Map<String, Set<String>> diagramRooms = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    private final Map<String, String> sessionDiagramMap = new ConcurrentHashMap<>();

    public WebSocketHandler(JwtService jwtService, UserRepository userRepository,
                            SimpMessagingTemplate messagingTemplate, ProjectAccessService accessService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
        this.accessService = accessService;
    }

    private String extractUserId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        // The STOMP principal is already set to the user id at CONNECT time.
        if (principal != null) {
            return principal.getName();
        }
        String token = headerAccessor.getFirstNativeHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtService.isTokenValid(token)) {
                String email = jwtService.extractEmail(token);
                if (email != null) {
                    return userRepository.findByEmail(email).map(u -> u.getId().value()).orElse(email);
                }
            }
        }
        return null;
    }

    @MessageMapping("/joinDiagram")
    public void joinDiagram(@Payload JoinDiagramMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null || message.diagramId() == null) return;

        String diagramId = message.diagramId();
        try {
            accessService.requireDiagramMemberByDiagramId(diagramId, userId);
        } catch (DomainException e) {
            return;
        }

        diagramRooms.computeIfAbsent(diagramId, k -> ConcurrentHashMap.newKeySet()).add(userId);

        String sessionId = headerAccessor.getSessionId();
        if (sessionId != null) {
            sessionUserMap.put(sessionId, userId);
            sessionDiagramMap.put(sessionId, diagramId);
        }

        broadcastPresence(diagramId, userId);
    }

    @MessageMapping("/leaveDiagram")
    public void leaveDiagram(@Payload LeaveDiagramMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null) return;

        String diagramId = message.diagramId();
        removeFromRoom(userId, diagramId);
        broadcastPresence(diagramId, userId);
    }

    @MessageMapping("/diagram/update")
    public void diagramUpdate(@Payload DiagramUpdateMessage message, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        String userId = extractUserId(principal, headerAccessor);
        if (userId == null || message.diagramId() == null) return;

        Set<String> room = diagramRooms.get(message.diagramId());
        if (room == null || !room.contains(userId)) return;

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("type", message.type());
        response.put("data", message.data());
        messagingTemplate.convertAndSend("/topic/diagram/" + message.diagramId() + "/changes", (Object) response);
    }

    private void broadcastPresence(String diagramId, String userId) {
        Set<String> users = diagramRooms.getOrDefault(diagramId, Collections.emptySet());
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("users", new ArrayList<>(users));
        messagingTemplate.convertAndSend("/topic/diagram/" + diagramId + "/presence", (Object) response);
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sessionUserMap.remove(sessionId);
        String diagramId = sessionDiagramMap.remove(sessionId);
        if (userId != null && diagramId != null) {
            removeFromRoom(userId, diagramId);
            broadcastPresence(diagramId, userId);
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

    public record JoinDiagramMessage(String diagramId) {}
    public record LeaveDiagramMessage(String diagramId) {}
    public record DiagramUpdateMessage(String diagramId, String type, Object data) {}
}
