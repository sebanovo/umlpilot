package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence.mongo;

import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoIndexInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataMongoTest
@Testcontainers(disabledWithoutDocker = true)
@Import({MongoGroupChatRepository.class, MongoChatSummaryRepository.class,
        MongoChatContextRetriever.class, ChatMongoIndexInitializer.class})
class MongoGroupChatRepositoryTest {

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("app.chat.index-initialization-enabled", () -> "true");
    }

    @Autowired
    private MongoGroupChatRepository repository;

    @Test
    void createsOneConversationPerProject() {
        var first = repository.getOrCreate("project-1", "user-1");
        var second = repository.getOrCreate("project-1", "user-2");

        assertEquals(first.id(), second.id());
    }

    @Test
    void duplicateClientMessageIsIdempotent() {
        var conversation = repository.getOrCreate("project-2", "user-1");
        var first = repository.saveMessage(message(conversation.id(), "Hola", "client-1"));
        var second = repository.saveMessage(message(conversation.id(), "Otro texto", "client-1"));

        assertEquals(first.id(), second.id());
        assertEquals("Hola", second.content());
    }

    @Test
    void paginatesAndSearchesOlderMessages() {
        var conversation = repository.getOrCreate("project-3", "user-1");
        repository.saveMessage(message(conversation.id(), "Primera decisión del equipo", "client-1"));
        repository.saveMessage(message(conversation.id(), "Segunda decisión importante", "client-2"));
        repository.saveMessage(message(conversation.id(), "Mensaje sin relación", "client-3"));

        List<?> page = repository.findMessagesBefore(conversation.id(), null, null, 2);
        assertEquals(2, page.size());

        List<?> results = repository.searchOlderMessages(conversation.id(), "decisión", null, 5);
        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 5);
    }

    private com.umlpilot.umlpilot_api.domain.model.ConversationMessage message(
            String conversationId, String content, String clientMessageId) {
        var author = com.umlpilot.umlpilot_api.domain.model.ChatAuthor.user("user-1", "Ana", "editor");
        return com.umlpilot.umlpilot_api.domain.model.ConversationMessage.user(
                conversationId, "project", null, author, content, clientMessageId);
    }
}
