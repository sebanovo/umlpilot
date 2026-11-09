package com.umlpilot.umlpilot_api.application.dto;

public record SendChatMessageCommand(
        String projectId,
        String userId,
        String clientMessageId,
        String content,
        String diagramId
) {
}
