package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatCitationParserTest {

    private final ChatCitationParser parser = new ChatCitationParser();

    @Test
    void extractsUniqueIds() {
        assertEquals(List.of("m1", "m2"),
                parser.extractIds("Según [msg:m1] y [msg:m2], también [msg:m1]."));
    }

    @Test
    void ignoresMissingIds() {
        ConversationMessage message = ConversationMessage.user(
                "c1", "p1", null, ChatAuthor.user("u1", "Ana", "editor"),
                "Decisión importante", "client-1").withId("m1");

        var citations = parser.resolve("Ver [msg:m1] y [msg:missing]", id ->
                "m1".equals(id) ? Optional.of(message) : Optional.empty());

        assertEquals(1, citations.size());
        assertEquals("m1", citations.get(0).messageId());
        assertEquals("Ana", citations.get(0).authorName());
    }

    @Test
    void returnsEmptyWithoutCitations() {
        assertTrue(parser.extractIds("Sin referencias").isEmpty());
        assertTrue(parser.extractIds(null).isEmpty());
    }
}
