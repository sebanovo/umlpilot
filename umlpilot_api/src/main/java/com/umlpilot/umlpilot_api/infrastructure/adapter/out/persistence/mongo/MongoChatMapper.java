package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.umlpilot.umlpilot_api.domain.model.ChatAction;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthorType;
import com.umlpilot.umlpilot_api.domain.model.ChatCitation;
import com.umlpilot.umlpilot_api.domain.model.ChatConversationStatus;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.ConversationSummary;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;

import java.util.List;

final class MongoChatMapper {

    private MongoChatMapper() {
    }

    static ConversationDocument toDocument(GroupConversation conversation) {
        ConversationDocument document = new ConversationDocument();
        document.setId(conversation.id());
        document.setProjectId(conversation.projectId());
        document.setTitle(conversation.title());
        document.setCreatedBy(conversation.createdBy());
        document.setCreatedAt(conversation.createdAt());
        document.setUpdatedAt(conversation.updatedAt());
        document.setLastMessageAt(conversation.lastMessageAt());
        document.setFocusDiagramId(conversation.focusDiagramId());
        document.setStatus(conversation.status().name());
        document.setSummaryText(conversation.summaryText());
        document.setSummaryThroughMessageId(conversation.summaryThroughMessageId());
        document.setSummaryUpdatedAt(conversation.summaryUpdatedAt());
        document.setUnsummarizedCount(conversation.unsummarizedCount());
        return document;
    }

    static GroupConversation toDomain(ConversationDocument document) {
        return new GroupConversation(
                document.getId(),
                document.getProjectId(),
                document.getTitle(),
                document.getCreatedBy(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                document.getLastMessageAt(),
                document.getFocusDiagramId(),
                document.getStatus() == null ? ChatConversationStatus.ACTIVE
                        : ChatConversationStatus.valueOf(document.getStatus()),
                document.getSummaryText(),
                document.getSummaryThroughMessageId(),
                document.getSummaryUpdatedAt(),
                document.getUnsummarizedCount());
    }

    static MessageDocument toDocument(ConversationMessage message) {
        MessageDocument document = new MessageDocument();
        document.setId(message.id());
        document.setConversationId(message.conversationId());
        document.setProjectId(message.projectId());
        document.setDiagramId(message.diagramId());
        document.setAuthorType(message.authorType().name());
        document.setAuthorUserId(message.author().userId());
        document.setAuthorName(message.author().name());
        document.setAuthorRole(message.author().role());
        document.setContent(message.content());
        document.setClientMessageId(message.clientMessageId());
        document.setReplyToId(message.replyToId());
        document.setCitations(message.citations().stream()
                .map(c -> new CitationDocument(c.messageId(), c.snippet(), c.authorName(), c.createdAt()))
                .toList());
        document.setActions(message.actions().stream()
                .map(a -> new ActionDocument(a.action(), a.entity(), a.id(), a.result()))
                .toList());
        document.setProvider(message.provider());
        document.setModel(message.model());
        document.setAiStatus(message.aiStatus().name());
        document.setCreatedAt(message.createdAt());
        document.setEditedAt(message.editedAt());
        document.setDeletedAt(message.deletedAt());
        return document;
    }

    static ConversationMessage toDomain(MessageDocument document) {
        List<ChatCitation> citations = document.getCitations() == null ? List.of()
                : document.getCitations().stream()
                .map(c -> new ChatCitation(c.getMessageId(), c.getSnippet(), c.getAuthorName(), c.getCreatedAt()))
                .toList();
        List<ChatAction> actions = document.getActions() == null ? List.of()
                : document.getActions().stream()
                .map(a -> new ChatAction(a.getAction(), a.getEntity(), a.getId(), a.getResult()))
                .toList();
        return new ConversationMessage(
                document.getId(),
                document.getConversationId(),
                document.getProjectId(),
                document.getDiagramId(),
                document.getAuthorType() == null ? ChatAuthorType.USER
                        : ChatAuthorType.valueOf(document.getAuthorType()),
                new ChatAuthor(document.getAuthorUserId(), document.getAuthorName(), document.getAuthorRole()),
                document.getContent(),
                document.getClientMessageId(),
                document.getReplyToId(),
                citations,
                actions,
                document.getProvider(),
                document.getModel(),
                document.getAiStatus() == null ? ChatMessageStatus.NONE
                        : ChatMessageStatus.valueOf(document.getAiStatus()),
                document.getCreatedAt(),
                document.getEditedAt(),
                document.getDeletedAt());
    }

    static SummaryDocument toDocument(ConversationSummary summary) {
        SummaryDocument document = new SummaryDocument();
        document.setId(summary.id());
        document.setConversationId(summary.conversationId());
        document.setFromMessageId(summary.fromMessageId());
        document.setToMessageId(summary.toMessageId());
        document.setText(summary.text());
        document.setModel(summary.model());
        document.setCreatedAt(summary.createdAt());
        return document;
    }

    static ConversationSummary toDomain(SummaryDocument document) {
        return new ConversationSummary(
                document.getId(),
                document.getConversationId(),
                document.getFromMessageId(),
                document.getToMessageId(),
                document.getText(),
                document.getModel(),
                document.getCreatedAt());
    }
}
