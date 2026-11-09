package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface MessageMongoRepository extends MongoRepository<MessageDocument, String> {

    Optional<MessageDocument> findByConversationIdAndClientMessageId(String conversationId, String clientMessageId);
}
