package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.umlpilot.umlpilot_api.application.port.out.ChatSummaryRepository;
import com.umlpilot.umlpilot_api.domain.model.ConversationSummary;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class MongoChatSummaryRepository implements ChatSummaryRepository {

    private final SummaryMongoRepository summaries;
    private final ConversationMongoRepository conversations;
    private final MongoTemplate mongoTemplate;

    public MongoChatSummaryRepository(SummaryMongoRepository summaries,
                                      ConversationMongoRepository conversations,
                                      MongoTemplate mongoTemplate) {
        this.summaries = summaries;
        this.conversations = conversations;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Optional<ConversationSummary> findLatest(String conversationId) {
        return summaries.findFirstByConversationIdOrderByCreatedAtDesc(conversationId)
                .map(MongoChatMapper::toDomain);
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        return MongoChatMapper.toDomain(
                summaries.save(MongoChatMapper.toDocument(summary)));
    }

    @Override
    public List<GroupConversation> findDueForSummary(int threshold, int limit) {
        return conversations.findByStatusAndUnsummarizedCountGreaterThanEqual("ACTIVE", threshold)
                .stream()
                .limit(Math.max(1, limit))
                .map(MongoChatMapper::toDomain)
                .toList();
    }

    @Override
    public void markSummarized(String conversationId, String text, String throughMessageId, Instant when) {
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(conversationId)),
                new Update()
                        .set("summaryText", text)
                        .set("summaryThroughMessageId", throughMessageId)
                        .set("summaryUpdatedAt", when)
                        .set("updatedAt", when)
                        .set("unsummarizedCount", 0),
                ConversationDocument.class);
    }
}
