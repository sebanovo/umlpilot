package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationMongoRepository extends MongoRepository<ConversationDocument, String> {

    Optional<ConversationDocument> findByProjectId(String projectId);

    List<ConversationDocument> findByStatusAndUnsummarizedCountGreaterThanEqual(String status, long threshold);
}
