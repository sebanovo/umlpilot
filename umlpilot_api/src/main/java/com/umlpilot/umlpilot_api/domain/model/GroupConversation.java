package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

import java.time.Instant;

/**
 * Conversación principal (grupal) de un proyecto. El agregado guarda metadatos y el
 * puntero al resumen; los mensajes viven en documentos independientes.
 */
public record GroupConversation(
        String id,
        String projectId,
        String title,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Instant lastMessageAt,
        String focusDiagramId,
        ChatConversationStatus status,
        String summaryText,
        String summaryThroughMessageId,
        Instant summaryUpdatedAt,
        long unsummarizedCount
) {

    public GroupConversation {
        if (projectId == null || projectId.isBlank()) {
            throw new DomainException("El proyecto de la conversación es requerido");
        }
        if (title == null || title.isBlank()) {
            title = "Chat del proyecto";
        }
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = createdAt;
        if (status == null) status = ChatConversationStatus.ACTIVE;
        if (unsummarizedCount < 0) unsummarizedCount = 0;
    }

    public static GroupConversation create(String projectId, String createdBy) {
        Instant now = Instant.now();
        return new GroupConversation(
                null, projectId, "Chat del proyecto", createdBy, now, now, now,
                null, ChatConversationStatus.ACTIVE, null, null, null, 0);
    }

    public GroupConversation withId(String newId) {
        return new GroupConversation(newId, projectId, title, createdBy, createdAt, updatedAt,
                lastMessageAt, focusDiagramId, status, summaryText, summaryThroughMessageId,
                summaryUpdatedAt, unsummarizedCount);
    }

    public GroupConversation withFocusDiagram(String diagramId) {
        return new GroupConversation(id, projectId, title, createdBy, createdAt, Instant.now(),
                lastMessageAt, diagramId, status, summaryText, summaryThroughMessageId,
                summaryUpdatedAt, unsummarizedCount);
    }

    public GroupConversation withLastMessage(Instant when) {
        return new GroupConversation(id, projectId, title, createdBy, createdAt, when,
                when, focusDiagramId, status, summaryText, summaryThroughMessageId,
                summaryUpdatedAt, unsummarizedCount);
    }

    public GroupConversation withSummary(String text, String throughMessageId, Instant when) {
        return new GroupConversation(id, projectId, title, createdBy, createdAt, when,
                lastMessageAt, focusDiagramId, status, text, throughMessageId,
                when, 0);
    }

    public GroupConversation archived() {
        return new GroupConversation(id, projectId, title, createdBy, createdAt, Instant.now(),
                lastMessageAt, focusDiagramId, ChatConversationStatus.ARCHIVED, summaryText,
                summaryThroughMessageId, summaryUpdatedAt, unsummarizedCount);
    }
}
