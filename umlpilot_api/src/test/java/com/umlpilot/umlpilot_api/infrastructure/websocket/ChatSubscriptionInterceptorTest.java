package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatSubscriptionInterceptorTest {

    private final ProjectAccessService accessService = mock(ProjectAccessService.class);
    private final ChatSubscriptionInterceptor interceptor = new ChatSubscriptionInterceptor(accessService);

    @Test
    void acceptsMemberSubscription() {
        Message<byte[]> message = subscribe("/topic/project/p1/chat", "u1");

        assertDoesNotThrow(() -> interceptor.preSend(message, null));
        verify(accessService).requireMember("p1", "u1");
    }

    @Test
    void rejectsNonMemberSubscription() {
        doThrow(new DomainException("No tienes acceso a este proyecto"))
                .when(accessService).requireMember("p1", "u2");
        Message<byte[]> message = subscribe("/topic/project/p1/chat", "u2");

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void ignoresOtherDestinations() {
        Message<byte[]> message = subscribe("/topic/diagram/d1/changes", "u1");

        assertDoesNotThrow(() -> interceptor.preSend(message, null));
        verifyNoInteractions(accessService);
    }

    private Message<byte[]> subscribe(String destination, String userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null));
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
