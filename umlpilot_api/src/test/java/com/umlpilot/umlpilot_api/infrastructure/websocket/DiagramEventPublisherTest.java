package com.umlpilot.umlpilot_api.infrastructure.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DiagramEventPublisherTest {

    @Test
    void publishesElementEventToDiagramChangesTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        DiagramEventPublisher publisher = new DiagramEventPublisher(template);

        publisher.elementDeleted("diagram-1", "user-1", "element-1");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/diagram/diagram-1/changes"), payload.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertEquals("user-1", body.get("userId"));
        assertEquals("element_deleted", body.get("type"));
        assertEquals("element-1", ((Map<?, ?>) body.get("data")).get("elementId"));
    }

    @Test
    void publishesRelationshipEventToDiagramChangesTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        DiagramEventPublisher publisher = new DiagramEventPublisher(template);

        publisher.relationshipCreated("diagram-2", "user-2", "rel-1");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/diagram/diagram-2/changes"), payload.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertEquals("relationship_created", body.get("type"));
        assertEquals("rel-1", ((Map<?, ?>) body.get("data")).get("relationshipId"));
    }
}
