package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Mensaje del chat grupal. Es inmutable: los cambios de estado se representan con
 * nuevas instancias y el adaptador actualiza solo los campos necesarios.
 */
public record ConversationMessage(
        String id,
        String conversationId,
        String projectId,
        String diagramId,
        ChatAuthorType authorType,
        ChatAuthor author,
        String content,
        String clientMessageId,
        String replyToId,
        List<ChatCitation> citations,
        List<ChatAction> actions,
        String provider,
        String model,
        ChatMessageStatus aiStatus,
        Instant createdAt,
        Instant editedAt,
        Instant deletedAt
) {

    public ConversationMessage {
        if (conversationId == null || conversationId.isBlank()) {
            throw new DomainException("La conversación del mensaje es requerida");
        }
        if (author == null) {
            throw new DomainException("El autor del mensaje es requerido");
        }
        if (authorType == null) {
            authorType = ChatAuthorType.USER;
        }
        if (content == null) content = "";
        if (citations == null) citations = List.of();
        if (actions == null) actions = List.of();
        if (aiStatus == null) aiStatus = ChatMessageStatus.NONE;
        if (createdAt == null) createdAt = Instant.now();
    }

    public static ConversationMessage user(String conversationId, String projectId, String diagramId,
                                           ChatAuthor author, String content, String clientMessageId) {
        if (content == null || content.isBlank()) {
            throw new DomainException("El mensaje es requerido");
        }
        return new ConversationMessage(null, conversationId, projectId, diagramId,
                ChatAuthorType.USER, author, content.trim(), clientMessageId, null,
                List.of(), List.of(), null, null, ChatMessageStatus.GENERATING, Instant.now(),
                null, null);
    }

    public static ConversationMessage assistant(String conversationId, String projectId, String diagramId,
                                                String content, String replyToId,
                                                List<ChatCitation> citations,
                                                List<ChatAction> actions,
                                                String provider, String model) {
        return new ConversationMessage(null, conversationId, projectId, diagramId,
                ChatAuthorType.ASSISTANT, ChatAuthor.assistant(), content == null ? "" : content,
                null, replyToId, citations, actions, provider, model,
                ChatMessageStatus.COMPLETED, Instant.now(), null, null);
    }

    public static ConversationMessage system(String conversationId, String projectId, String content) {
        return new ConversationMessage(null, conversationId, projectId, null,
                ChatAuthorType.SYSTEM, ChatAuthor.system(), content, null, null,
                List.of(), List.of(), null, null, ChatMessageStatus.NONE, Instant.now(),
                null, null);
    }

    public ConversationMessage withId(String newId) {
        return new ConversationMessage(newId, conversationId, projectId, diagramId, authorType,
                author, content, clientMessageId, replyToId, citations, actions, provider,
                model, aiStatus, createdAt, editedAt, deletedAt);
    }

    public ConversationMessage withStatus(ChatMessageStatus status) {
        return new ConversationMessage(id, conversationId, projectId, diagramId, authorType,
                author, content, clientMessageId, replyToId, citations, actions, provider,
                model, status, createdAt, editedAt, deletedAt);
    }

    public boolean isUserMessage() {
        return authorType == ChatAuthorType.USER;
    }

    public Map<String, Object> actionMap(ChatAction action) {
        return Map.of(
                "action", action.action(),
                "entity", action.entity(),
                "id", action.id() == null ? "" : action.id(),
                "result", action.result() == null ? "" : action.result());
    }
}
