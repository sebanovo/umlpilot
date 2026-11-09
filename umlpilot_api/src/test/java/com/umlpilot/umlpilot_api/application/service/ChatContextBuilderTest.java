package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ChatAiMessage;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.domain.model.GroupConversation;
import com.umlpilot.umlpilot_api.infrastructure.config.ChatMongoProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatContextBuilderTest {

    private final ChatContextBuilder builder = new ChatContextBuilder(new ChatMongoProperties());

    @Test
    void includesAuthorRoleSummaryAndRetrievedCitations() {
        GroupConversation conversation = GroupConversation.create("p1", "u1")
                .withId("c1")
                .withSummary("Se decidió usar Cliente y Pedido.", "m-old", Instant.now());
        ConversationMessage current = ConversationMessage.user(
                "c1", "p1", "d1", ChatAuthor.user("u1", "Ana", "editor"),
                "¿Qué decidimos sobre Pedido?", "client-1").withId("m-current");
        ConversationMessage recent = ConversationMessage.assistant(
                "c1", "p1", "d1", "Usamos una asociación.", "m-prev",
                null, null, "gemini", "gemini-3.8-flash").withId("m-recent");
        ConversationMessage old = ConversationMessage.user(
                "c1", "p1", "d1", ChatAuthor.user("u2", "Luis", "viewer"),
                "Propongo Pedido con fecha.", "client-old").withId("m-old");

        List<ChatAiMessage> context = builder.build(
                conversation, current, List.of(recent), List.of(old), false);

        String all = context.stream().map(ChatAiMessage::content).reduce("", (a, b) -> a + "\n" + b);
        assertTrue(all.contains("Se decidió usar Cliente y Pedido."));
        assertTrue(all.contains("[Ana (editor)]: ¿Qué decidimos sobre Pedido?"));
        assertTrue(all.contains("[msg:m-old] [Luis (viewer)]"));
        assertTrue(all.contains("No puedes crear, editar ni eliminar"));
    }

    @Test
    void includesMutationPermissionForEditor() {
        GroupConversation conversation = GroupConversation.create("p1", "u1").withId("c1");
        ConversationMessage current = ConversationMessage.user(
                "c1", "p1", "d1", ChatAuthor.user("u1", "Ana", "owner"),
                "Crea una clase", "client-1").withId("m-1");

        List<ChatAiMessage> context = builder.build(conversation, current, List.of(), List.of(), true);
        String all = context.stream().map(ChatAiMessage::content).reduce("", (a, b) -> a + "\n" + b);

        assertTrue(all.contains("Puedes ejecutar las tools de lectura y mutación"));
    }
}
