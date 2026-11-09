package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

import java.time.Instant;

public record ConversationSummary(
        String id,
        String conversationId,
        String fromMessageId,
        String toMessageId,
        String text,
        String model,
        Instant createdAt
) {
    public ConversationSummary {
        if (conversationId == null || conversationId.isBlank()) {
            throw new DomainException("La conversación del resumen es requerida");
        }
        if (text == null || text.isBlank()) {
            throw new DomainException("El texto del resumen es requerido");
        }
        if (createdAt == null) createdAt = Instant.now();
    }
}
