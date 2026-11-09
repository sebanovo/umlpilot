package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;

import java.util.List;

public interface ChatEventPublisher {

    void messageCreated(String projectId, ConversationMessage message);

    void aiStatus(String projectId, String messageId, ChatMessageStatus status, String error);

    void typing(String projectId, String userId, String name, boolean typing);

    void presence(String projectId, List<String> userIds);
}
