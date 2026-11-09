package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

public record ChatAiRequest(
        String projectId,
        String diagramId,
        String userId,
        String authorName,
        String authorRole,
        boolean allowMutations,
        boolean toolsEnabled,
        List<ChatAiMessage> messages
) {
}
