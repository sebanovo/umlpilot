package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request;

public record SendChatMessageRequest(
        String clientMessageId,
        String content,
        String diagramId
) {
}
