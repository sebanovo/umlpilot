package com.umlpilot.umlpilot_api.application.dto;

import com.umlpilot.umlpilot_api.domain.model.ChatCitation;

import java.time.Instant;

public record ChatCitationResult(String messageId, String snippet, String authorName, Instant createdAt) {

    public static ChatCitationResult from(ChatCitation citation) {
        return new ChatCitationResult(citation.messageId(), citation.snippet(),
                citation.authorName(), citation.createdAt());
    }
}
