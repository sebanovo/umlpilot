package com.umlpilot.umlpilot_api.infrastructure.adapter.in.web;

import com.umlpilot.umlpilot_api.application.dto.ChatConversationResult;
import com.umlpilot.umlpilot_api.application.dto.ChatMessageResult;
import com.umlpilot.umlpilot_api.application.dto.ChatSendResult;
import com.umlpilot.umlpilot_api.application.service.GroupChatService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.ChatAuthor;
import com.umlpilot.umlpilot_api.domain.model.ConversationMessage;
import com.umlpilot.umlpilot_api.infrastructure.adapter.in.web.request.SendChatMessageRequest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class GroupChatControllerTest {

    private final GroupChatService service = mock(GroupChatService.class);
    private final GroupChatController controller = new GroupChatController(service);

    @Test
    void conversationReturns200() {
        when(service.getOrCreateConversation("p1", "u1")).thenReturn(
                new ChatConversationResult("c1", "p1", "Chat", null, null, true,
                        Instant.now(), 0));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.conversation("p1");

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    void newMessageReturns202() {
        when(service.sendMessage(any())).thenReturn(new ChatSendResult(message(), false));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.send("p1",
                    new SendChatMessageRequest("client-1", "Hola", null));

            assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        }
    }

    @Test
    void duplicateMessageReturns200() {
        when(service.sendMessage(any())).thenReturn(new ChatSendResult(message(), true));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.send("p1",
                    new SendChatMessageRequest("client-1", "Hola", null));

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Test
    void blankMessageReturns400() {
        when(service.sendMessage(any())).thenThrow(new DomainException("El mensaje es requerido"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.send("p1",
                    new SendChatMessageRequest("client-1", " ", null));

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Test
    void nonMemberReturns403() {
        when(service.getOrCreateConversation("p1", "u1"))
                .thenThrow(new DomainException("No tienes acceso a este proyecto"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.conversation("p1");

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Test
    void missingProjectReturns404() {
        when(service.getOrCreateConversation("p1", "u1"))
                .thenThrow(new DomainException("Proyecto no encontrado"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.conversation("p1");

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Test
    void storeFailureReturns503() {
        when(service.getOrCreateConversation("p1", "u1"))
                .thenThrow(new DataAccessResourceFailureException("mongo down"));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getCurrentUserId).thenReturn("u1");
            ResponseEntity<?> response = controller.conversation("p1");

            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        }
    }

    private ChatMessageResult message() {
        ConversationMessage message = ConversationMessage.user(
                "c1", "p1", null, ChatAuthor.user("u1", "Ana", "editor"),
                "Hola", "client-1").withId("m1");
        return ChatMessageResult.from(message);
    }
}
