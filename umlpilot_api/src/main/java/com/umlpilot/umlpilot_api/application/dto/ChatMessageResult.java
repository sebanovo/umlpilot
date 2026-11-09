package com.umlpilot.umlpilot_api.application.dto;

import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;

import java.time.Instant;
import java.util.List;

public record ChatMessageResult(
        String id,
        String conversationId,
        String projectId,
        String diagramId,
        String authorType,
        String authorId,
        String authorName,
        String authorRole,
        String content,
        String clientMessageId,
        String replyToId,
        List<ChatCitationResult> citations,
        List<ChatActionResult> actions,
        String provider,
        String model,
        String aiStatus,
        Instant createdAt
) {

    public static ChatMessageResult from(ConversationMessage message) {
        return new ChatMessageResult(
                message.id(),
                message.conversationId(),
                message.projectId(),
                message.diagramId(),
                message.authorType().name().toLowerCase(),
                message.author().userId(),
                message.author().name(),
                message.author().role(),
                message.content(),
                message.clientMessageId(),
                message.replyToId(),
                message.citations().stream().map(ChatCitationResult::from).toList(),
                message.actions().stream().map(ChatActionResult::from).toList(),
                message.provider(),
                message.model(),
                message.aiStatus().name().toLowerCase(),
                message.createdAt());
    }
}
