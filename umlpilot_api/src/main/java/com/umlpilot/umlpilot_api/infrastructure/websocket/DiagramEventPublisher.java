package com.umlpilot.umlpilot_api.infrastructure.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Publica cambios de diagrama en el mismo canal que usan los clientes STOMP
 * ({@code /topic/diagram/{diagramId}/changes}) para que los cambios originados en el
 * servidor (por ejemplo, el asistente de IA) se sincronicen en tiempo real.
 */
@Component
public class DiagramEventPublisher {

    public static final String CHANGES_TOPIC = "/topic/diagram/%s/changes";

    private final SimpMessagingTemplate messagingTemplate;

    public DiagramEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(String diagramId, String userId, String type, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("type", type);
        payload.put("data", data);
        messagingTemplate.convertAndSend(CHANGES_TOPIC.formatted(diagramId), (Object) payload);
    }

    public void elementCreated(String diagramId, String userId, String elementId) {
        publish(diagramId, userId, "element_created", Map.of("elementId", elementId));
    }

    public void elementUpdated(String diagramId, String userId, String elementId) {
        publish(diagramId, userId, "element_updated", Map.of("elementId", elementId));
    }

    public void elementDeleted(String diagramId, String userId, String elementId) {
        publish(diagramId, userId, "element_deleted", Map.of("elementId", elementId));
    }

    public void relationshipCreated(String diagramId, String userId, String relationshipId) {
        publish(diagramId, userId, "relationship_created", Map.of("relationshipId", relationshipId));
    }

    public void relationshipUpdated(String diagramId, String userId, String relationshipId) {
        publish(diagramId, userId, "relationship_updated", Map.of("relationshipId", relationshipId));
    }

    public void relationshipDeleted(String diagramId, String userId, String relationshipId) {
        publish(diagramId, userId, "relationship_deleted", Map.of("relationshipId", relationshipId));
    }

    /** Cambio masivo (p. ej. construir el diagrama desde imagen/texto): refresca todo. */
    public void diagramChanged(String diagramId, String userId) {
        publish(diagramId, userId, "diagram_changed", Map.of("diagramId", diagramId));
    }

    /** Cambio masivo indicando el origen (p. ej. {@code import}): refresca todo el diagrama. */
    public void diagramChanged(String diagramId, String userId, String origin) {
        publish(diagramId, userId, "diagram_changed", Map.of("diagramId", diagramId, "type", origin));
    }
}
