package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.event.ProjectDeletedEvent;
import com.umlpilot.umlpilot_api.application.port.out.GroupChatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ChatCleanupListener {

    private static final Logger log = LoggerFactory.getLogger(ChatCleanupListener.class);

    private final GroupChatRepository chatRepository;

    public ChatCleanupListener(GroupChatRepository chatRepository) {
        this.chatRepository = chatRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProjectDeleted(ProjectDeletedEvent event) {
        try {
            chatRepository.archiveByProject(event.projectId());
        } catch (RuntimeException e) {
            // La membresía ya impide el acceso; una limpieza posterior puede reintentarse.
            log.warn("No se pudo archivar el chat del proyecto {}: {}", event.projectId(), e.getMessage());
        }
    }
}
