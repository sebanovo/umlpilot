package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SummaryMongoRepository extends MongoRepository<SummaryDocument, String> {

    Optional<SummaryDocument> findFirstByConversationIdOrderByCreatedAtDesc(String conversationId);
}
