package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import com.umlpilot.umlpilot_api.domain.model.ChatMessageStatus;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class MongoGroupChatRepository implements GroupChatRepository {

    private final ConversationMongoRepository conversations;
    private final MessageMongoRepository messages;
    private final MongoTemplate mongoTemplate;

    public MongoGroupChatRepository(ConversationMongoRepository conversations,
                                    MessageMongoRepository messages,
                                    MongoTemplate mongoTemplate) {
        this.conversations = conversations;
        this.messages = messages;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<GroupConversation> findByProjectId(String projectId) {
        return conversations.findByProjectId(projectId).map(MongoChatMapper::toDomain);
    }

    @Override
    public GroupConversation getOrCreate(String projectId, String createdBy) {
        Optional<ConversationDocument> existing = conversations.findByProjectId(projectId);
        if (existing.isPresent()) {
            return MongoChatMapper.toDomain(existing.get());
        }
        try {
            ConversationDocument saved = conversations.save(
                    MongoChatMapper.toDocument(GroupConversation.create(projectId, createdBy)));
            return MongoChatMapper.toDomain(saved);
        } catch (DuplicateKeyException duplicate) {
            return conversations.findByProjectId(projectId)
                    .map(MongoChatMapper::toDomain)
                    .orElseThrow(() -> duplicate);
        }
    }

    @Override
    public GroupConversation save(GroupConversation conversation) {
        return MongoChatMapper.toDomain(
                conversations.save(MongoChatMapper.toDocument(conversation)));
    }

    @Override
    public ConversationMessage saveMessage(ConversationMessage message) {
        try {
            return MongoChatMapper.toDomain(
                    messages.save(MongoChatMapper.toDocument(message)));
        } catch (DuplicateKeyException duplicate) {
            return findMessageByClientId(message.conversationId(), message.clientMessageId())
                    .orElseThrow(() -> duplicate);
        }
    }

    @Override
    public Optional<ConversationMessage> findMessageById(String messageId) {
        return messages.findById(messageId).map(MongoChatMapper::toDomain);
    }

    @Override
    public Optional<ConversationMessage> findMessageByClientId(String conversationId, String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) return Optional.empty();
        return messages.findByConversationIdAndClientMessageId(conversationId, clientMessageId)
                .map(MongoChatMapper::toDomain);
    }

    @Override
    public List<ConversationMessage> findRecentMessages(String conversationId, int limit) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        query.with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"));
        query.limit(Math.max(1, limit));
        List<ConversationMessage> result = new ArrayList<>(mongoTemplate
                .find(query, MessageDocument.class).stream()
                .map(MongoChatMapper::toDomain)
                .toList());
        Collections.reverse(result);
        return result;
    }

    @Override
    public List<ConversationMessage> findMessagesBefore(String conversationId, Instant before, String beforeId,
                                                        int limit) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        if (before != null) {
            Criteria cursor = new Criteria().orOperator(
                    Criteria.where("createdAt").lt(before),
                    new Criteria().andOperator(
                            Criteria.where("createdAt").is(before),
                            Criteria.where("_id").lt(beforeId)));
            query.addCriteria(cursor);
        }
        query.with(Sort.by(Sort.Direction.DESC, "createdAt", "_id"));
        query.limit(Math.max(1, limit));
        return mongoTemplate.find(query, MessageDocument.class).stream()
                .map(MongoChatMapper::toDomain)
                .toList();
    }

    @Override
    public List<ConversationMessage> findMessagesAfter(String conversationId, Instant after, int limit) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        if (after != null) {
            query.addCriteria(Criteria.where("createdAt").gt(after));
        }
        query.with(Sort.by(Sort.Direction.ASC, "createdAt", "_id"));
        query.limit(Math.max(1, limit));
        return mongoTemplate.find(query, MessageDocument.class).stream()
                .map(MongoChatMapper::toDomain)
                .toList();
    }

    @Override
    public List<ConversationMessage> searchOlderMessages(String conversationId, String text, Instant before,
                                                         int limit) {
        if (text == null || text.isBlank()) return List.of();
        Query query = new Query();
        query.addCriteria(TextCriteria.forDefaultLanguage().matching(text));
        query.addCriteria(Criteria.where("conversationId").is(conversationId));
        if (before != null) {
            query.addCriteria(Criteria.where("createdAt").lt(before));
        }
        query.with(Sort.by(Sort.Direction.DESC, "createdAt"));
        query.limit(Math.max(1, limit));
        try {
            return mongoTemplate.find(query, MessageDocument.class).stream()
                    .map(MongoChatMapper::toDomain)
                    .toList();
        } catch (RuntimeException noTextIndex) {
            return List.of();
        }
    }

    @Override
    public void updateMessageStatus(String messageId, ChatMessageStatus status) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(messageId)),
                Update.update("aiStatus", status.name()),
                MessageDocument.class);
    }

    @Override
    public void incrementUnsummarized(String conversationId) {
        Instant now = Instant.now();
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(conversationId)),
                new Update().inc("unsummarizedCount", 1)
                        .set("lastMessageAt", now)
                        .set("updatedAt", now),
                ConversationDocument.class);
    }

    @Override
    public void archiveByProject(String projectId) {
        mongoTemplate.updateMulti(
                Query.query(Criteria.where("projectId").is(projectId)),
                new Update().set("status", "ARCHIVED").set("updatedAt", Instant.now()),
                ConversationDocument.class);
    }
}
