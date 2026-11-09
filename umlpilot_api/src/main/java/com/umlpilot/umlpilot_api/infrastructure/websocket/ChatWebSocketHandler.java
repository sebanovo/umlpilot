package com.umlpilot.umlpilot_api.infrastructure.websocket;

import com.umlpilot.umlpilot_api.application.port.out.ChatEventPublisher;
import com.umlpilot.umlpilot_api.application.service.ProjectAccessService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Controller
public class ChatWebSocketHandler implements ApplicationListener<SessionDisconnectEvent> {

    private final ProjectAccessService accessService;
    private final UserRepository userRepository;
    private final ChatPresenceRegistry presenceRegistry;
    private final ChatEventPublisher eventPublisher;

    public ChatWebSocketHandler(ProjectAccessService accessService,
                                UserRepository userRepository,
                                ChatPresenceRegistry presenceRegistry,
                                ChatEventPublisher eventPublisher) {
        this.accessService = accessService;
        this.userRepository = userRepository;
        this.presenceRegistry = presenceRegistry;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/chat/typing")
    public void typing(@Payload ChatActivityMessage message, Principal principal) {
        String userId = userId(principal);
        if (userId == null || message == null || message.projectId() == null) return;
        try {
            accessService.requireMember(message.projectId(), userId);
        } catch (DomainException e) {
            return;
        }
        eventPublisher.typing(message.projectId(), userId, nameOf(userId), message.typing());
    }

    @MessageMapping("/chat/presence")
    public void presence(@Payload ChatPresenceMessage message, Principal principal,
                         SimpMessageHeaderAccessor headerAccessor) {
        String userId = userId(principal);
        if (userId == null || message == null || message.projectId() == null) return;
        try {
            accessService.requireMember(message.projectId(), userId);
        } catch (DomainException e) {
            return;
        }
        presenceRegistry.join(message.projectId(), userId, headerAccessor.getSessionId());
        eventPublisher.presence(message.projectId(), presenceRegistry.users(message.projectId()));
    }

    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String projectId = presenceRegistry.projectOf(sessionId);
        presenceRegistry.leave(sessionId);
        if (projectId != null) {
            eventPublisher.presence(projectId, presenceRegistry.users(projectId));
        }
    }

    private String userId(Principal principal) {
        return principal == null ? null : principal.getName();
    }

    private String nameOf(String userId) {
        return userRepository.findById(UserId.from(userId))
                .map(user -> ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                        + (user.getLastName() == null ? "" : user.getLastName())).trim())
                .filter(name -> !name.isBlank())
                .orElse("Colaborador");
    }

    public record ChatActivityMessage(String projectId, boolean typing) {
    }

    public record ChatPresenceMessage(String projectId) {
    }
}
