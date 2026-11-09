package com.umlpilot.umlpilot_api.application.port.out;

import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface GroupChatRepository {

    Optional<GroupConversation> findByProjectId(String projectId);

    GroupConversation getOrCreate(String projectId, String createdBy);

    GroupConversation save(GroupConversation conversation);

    ConversationMessage saveMessage(ConversationMessage message);

    Optional<ConversationMessage> findMessageById(String messageId);

    Optional<ConversationMessage> findMessageByClientId(String conversationId, String clientMessageId);

    List<ConversationMessage> findRecentMessages(String conversationId, int limit);

    List<ConversationMessage> findMessagesBefore(String conversationId, Instant before, String beforeId, int limit);

    List<ConversationMessage> findMessagesAfter(String conversationId, Instant after, int limit);

    List<ConversationMessage> searchOlderMessages(String conversationId, String query, Instant before, int limit);

    void updateMessageStatus(String messageId, ChatMessageStatus status);

    void incrementUnsummarized(String conversationId);

    void archiveByProject(String projectId);
}
