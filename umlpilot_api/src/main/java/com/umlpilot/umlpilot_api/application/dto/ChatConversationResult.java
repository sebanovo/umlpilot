package com.umlpilot.umlpilot_api.application.dto;

import java.time.Instant;

public record ChatConversationResult(
        String conversationId,
        String projectId,
        String title,
        String summary,
        String focusDiagramId,
        boolean canMutate,
        Instant lastMessageAt,
        long unsummarizedCount
) {
}
