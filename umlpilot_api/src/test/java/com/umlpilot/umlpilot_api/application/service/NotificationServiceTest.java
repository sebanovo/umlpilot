package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.NotificationResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.CollaboratorId;
import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.NotificationId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final CollaboratorService collaboratorService = mock(CollaboratorService.class);
    private final NotificationService service = new NotificationService(
            notificationRepository, collaboratorRepository, collaboratorService);

    private Notification invitation() {
        return Notification.projectInvitation("bob1", "p1", "Sistema", "Ana Paz", "editor");
    }

    @Test
    void listsNotificationsWithInvitationStatus() {
        Notification notification = invitation();
        when(notificationRepository.findByUserId("bob1", false)).thenReturn(List.of(notification));
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.of(
                Collaborator.reconstitute(new CollaboratorId("p1", "bob1"), "editor", "pending",
                        LocalDateTime.now(), null)));

        List<NotificationResult> results = service.getNotifications("bob1", false);

        assertEquals(1, results.size());
        assertEquals("pending", results.get(0).invitationStatus());
        assertEquals("editor", results.get(0).role());
        assertFalse(results.get(0).read());
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(notificationRepository.countUnread("bob1")).thenReturn(3L);
        assertEquals(3L, service.getUnreadCount("bob1"));
    }

    @Test
    void markAsReadSetsReadAt() {
        Notification notification = invitation();
        when(notificationRepository.findByIdAndUserId(NotificationId.from(notification.getId().value()), "bob1"))
                .thenReturn(Optional.of(notification));

        service.markAsRead(notification.getId().value(), "bob1");

        assertTrue(notification.isRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    void respondAcceptDelegatesAndMarksRead() {
        Notification notification = invitation();
        when(notificationRepository.findByIdAndUserId(NotificationId.from(notification.getId().value()), "bob1"))
                .thenReturn(Optional.of(notification));

        service.respondToInvitation(notification.getId().value(), "bob1", "accept");

        verify(collaboratorService).acceptInvitation("p1", "bob1");
        assertTrue(notification.isRead());
        verify(notificationRepository).save(notification);
    }

    @Test
    void respondRejectDelegates() {
        Notification notification = invitation();
        when(notificationRepository.findByIdAndUserId(NotificationId.from(notification.getId().value()), "bob1"))
                .thenReturn(Optional.of(notification));

        service.respondToInvitation(notification.getId().value(), "bob1", "reject");

        verify(collaboratorService).rejectInvitation("p1", "bob1");
        verify(notificationRepository).save(notification);
    }

    @Test
    void invalidActionIsRejected() {
        Notification notification = invitation();
        when(notificationRepository.findByIdAndUserId(NotificationId.from(notification.getId().value()), "bob1"))
                .thenReturn(Optional.of(notification));

        assertThrows(DomainException.class,
                () -> service.respondToInvitation(notification.getId().value(), "bob1", "maybe"));
        verify(collaboratorService, never()).acceptInvitation(anyString(), anyString());
    }

    @Test
    void unknownNotificationIsRejected() {
        when(notificationRepository.findByIdAndUserId(any(), anyString())).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> service.markAsRead("nope", "bob1"));
        verify(notificationRepository, never()).save(any());
    }
}
