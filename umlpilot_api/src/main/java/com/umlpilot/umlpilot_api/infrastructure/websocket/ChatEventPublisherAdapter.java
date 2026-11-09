package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.dto.ChatMessageResult;
import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatEventPublisherAdapter implements ChatEventPublisher {

    public static final String TOPIC = "/topic/project/%s/chat";

    private final SimpMessagingTemplate messagingTemplate;

    public ChatEventPublisherAdapter(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void messageCreated(String projectId, ConversationMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat_message_created");
        payload.put("projectId", projectId);
        payload.put("message", ChatMessageResult.from(message));
        send(projectId, payload);
    }

    @Override
    public void aiStatus(String projectId, String messageId, ChatMessageStatus status, String error) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat_ai_status");
        payload.put("projectId", projectId);
        payload.put("messageId", messageId);
        payload.put("status", status.name().toLowerCase());
        if (error != null) payload.put("error", error);
        send(projectId, payload);
    }

    @Override
    public void typing(String projectId, String userId, String name, boolean typing) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat_typing");
        payload.put("projectId", projectId);
        payload.put("userId", userId);
        payload.put("name", name);
        payload.put("typing", typing);
        send(projectId, payload);
    }

    @Override
    public void presence(String projectId, List<String> userIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "chat_presence");
        payload.put("projectId", projectId);
        payload.put("users", userIds);
        send(projectId, payload);
    }

    private void send(String projectId, Map<String, Object> payload) {
        messagingTemplate.convertAndSend(TOPIC.formatted(projectId), (Object) payload);
    }
}
