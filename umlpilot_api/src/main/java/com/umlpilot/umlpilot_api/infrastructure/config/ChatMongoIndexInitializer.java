package com.umlpilot.umlpilot_api.infrastructure.config;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.chat.index-initialization-enabled",
        havingValue = "true", matchIfMissing = true)
public class ChatMongoIndexInitializer {

    private static final Logger log = LoggerFactory.getLogger(ChatMongoIndexInitializer.class);

    private final MongoTemplate mongoTemplate;

    public ChatMongoIndexInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initialize() {
        try {
            var db = mongoTemplate.getDb();
            db.getCollection("conversations").createIndex(
                    Indexes.ascending("projectId"),
                    new IndexOptions().unique(true).name("uq_conversations_project"));
            db.getCollection("conversations").createIndex(
                    Indexes.compoundIndex(Indexes.ascending("status"), Indexes.descending("lastMessageAt")),
                    new IndexOptions().name("ix_conversations_status_last_message"));
            db.getCollection("messages").createIndex(
                    Indexes.compoundIndex(Indexes.ascending("conversationId"), Indexes.descending("createdAt"),
                            Indexes.descending("_id")),
                    new IndexOptions().name("ix_messages_conversation_cursor"));
            db.getCollection("messages").createIndex(
                    Indexes.compoundIndex(Indexes.ascending("conversationId"), Indexes.ascending("aiStatus")),
                    new IndexOptions().name("ix_messages_conversation_status"));
            db.getCollection("messages").createIndex(
                    Indexes.compoundIndex(Indexes.ascending("conversationId"), Indexes.ascending("clientMessageId")),
                    new IndexOptions().unique(true)
                            .partialFilterExpression(Filters.exists("clientMessageId", true))
                            .name("uq_messages_client_id"));
            db.getCollection("messages").createIndex(
                    Indexes.ascending("content"),
                    new IndexOptions().name("tx_messages_content"));
            db.getCollection("summaries").createIndex(
                    Indexes.compoundIndex(Indexes.ascending("conversationId"), Indexes.descending("createdAt")),
                    new IndexOptions().name("ix_summaries_conversation_created"));
        } catch (RuntimeException e) {
            // El chat queda disponible cuando Mongo vuelva; no impedimos el arranque del resto.
            log.warn("No se pudieron crear los índices de MongoDB para el chat: {}", e.getMessage());
        }
    }
}
