package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ChatEventPublisherAdapterTest {

    @Test
    void publishesMessageToProjectChatTopic() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ChatEventPublisherAdapter publisher = new ChatEventPublisherAdapter(template);
        ConversationMessage message = ConversationMessage.user(
                "c1", "p1", null, ChatAuthor.user("u1", "Ana", "editor"),
                "Hola", "client-1").withId("m1");

        publisher.messageCreated("p1", message);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/project/p1/chat"), payload.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertEquals("chat_message_created", body.get("type"));
        assertEquals("p1", body.get("projectId"));
    }

    @Test
    void publishesAiStatus() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ChatEventPublisherAdapter publisher = new ChatEventPublisherAdapter(template);

        publisher.aiStatus("p1", "m1", ChatMessageStatus.FAILED, "error");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(template).convertAndSend(eq("/topic/project/p1/chat"), payload.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) payload.getValue();
        assertEquals("chat_ai_status", body.get("type"));
        assertEquals("failed", body.get("status"));
    }

    @Test
    void publishesPresence() {
        SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
        ChatEventPublisherAdapter publisher = new ChatEventPublisherAdapter(template);

        publisher.presence("p1", List.of("u1", "u2"));

        verify(template).convertAndSend(eq("/topic/project/p1/chat"), (Object) org.mockito.ArgumentMatchers.any());
    }
}
