package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
public class ChatSubscriptionInterceptor implements ChannelInterceptor {

    private static final String PREFIX = "/topic/project/";
    private static final String SUFFIX = "/chat";

    private final ProjectAccessService accessService;

    public ChatSubscriptionInterceptor(ProjectAccessService accessService) {
        this.accessService = accessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(PREFIX) || !destination.endsWith(SUFFIX)) {
            return message;
        }

        String projectId = destination.substring(PREFIX.length(), destination.length() - SUFFIX.length());
        String userId = accessor.getUser() == null ? null : accessor.getUser().getName();
        if (userId == null) {
            throw new MessagingException("No autenticado");
        }
        try {
            accessService.requireMember(projectId, userId);
        } catch (DomainException e) {
            throw new MessagingException("No tienes acceso al chat de este proyecto");
        }
        return message;
    }
}
