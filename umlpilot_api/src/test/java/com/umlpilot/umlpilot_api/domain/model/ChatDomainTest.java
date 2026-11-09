package com.umlpilot.umlpilot_api.domain.model;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatDomainTest {

    @Test
    void rejectsBlankUserMessage() {
        ChatAuthor author = ChatAuthor.user("u1", "Ana", "editor");
        assertThrows(DomainException.class, () ->
                ConversationMessage.user("c1", "p1", null, author, "   ", "client-1"));
    }

    @Test
    void rejectsAuthorWithoutName() {
        assertThrows(DomainException.class, () -> new ChatAuthor("u1", " ", "editor"));
    }

    @Test
    void createsConversationWithDefaults() {
        GroupConversation conversation = GroupConversation.create("p1", "u1");

        assertEquals("p1", conversation.projectId());
        assertEquals(ChatConversationStatus.ACTIVE, conversation.status());
        assertEquals(0, conversation.unsummarizedCount());
        assertEquals("Chat del proyecto", conversation.title());
    }

    @Test
    void userMessageStartsGenerating() {
        ChatAuthor author = ChatAuthor.user("u1", "Ana", "editor");
        ConversationMessage message = ConversationMessage.user(
                "c1", "p1", "d1", author, "Hola equipo", "client-1");

        assertEquals(ChatMessageStatus.GENERATING, message.aiStatus());
        assertEquals(ChatAuthorType.USER, message.authorType());
        assertEquals("Hola equipo", message.content());
    }

    @Test
    void assistantMessageIsCompleted() {
        ConversationMessage message = ConversationMessage.assistant(
                "c1", "p1", "d1", "Listo", "u-msg", null, null, "gemini", "gemini-3.8-flash");

        assertEquals(ChatMessageStatus.COMPLETED, message.aiStatus());
        assertEquals(ChatAuthorType.ASSISTANT, message.authorType());
        assertEquals("gemini", message.provider());
    }
}
