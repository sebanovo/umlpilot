package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CollaboratorResult;
import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.CollaboratorId;
import com.umlpilot.umlpilot_api.domain.model.Notification;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.Role;
import com.umlpilot.umlpilot_api.domain.model.User;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CollaboratorServiceInviteTest {

    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CollaboratorService service = new CollaboratorService(
            collaboratorRepository, projectRepository, userRepository, notificationRepository);

    private final LocalDateTime now = LocalDateTime.now();
    private final Project project = Project.reconstitute(ProjectId.from("p1"), "Sistema", "desc",
            UserId.from("owner1"), "active", now, now);
    private final User inviter = User.reconstitute(UserId.from("owner1"), "ana@x.com", "hash",
            "Ana", "Paz", Role.USER, now, now);
    private final User invitee = User.reconstitute(UserId.from("bob1"), "bob@x.com", "hash",
            "Bob", "Rey", Role.USER, now, now);

    private void stubInviter() {
        when(projectRepository.findById(ProjectId.from("p1"))).thenReturn(Optional.of(project));
        when(collaboratorRepository.findAcceptedCollaborator("p1", "owner1"))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", "owner1", "owner")));
        when(userRepository.findById(UserId.from("owner1"))).thenReturn(Optional.of(inviter));
    }

    @Test
    void invitesByEmailAndCreatesUnreadNotification() {
        stubInviter();
        when(userRepository.findByEmail("bob@x.com")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.empty());

        CollaboratorResult result = service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "  bob@x.com  ", "editor", "owner1"));

        assertEquals("pending", result.invitationStatus());
        assertEquals("editor", result.role());
        assertEquals("bob@x.com", result.email());
        verify(collaboratorRepository).save(any(Collaborator.class));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification notification = captor.getValue();
        assertEquals("bob1", notification.getUserId());
        assertEquals("p1", notification.getProjectId());
        assertEquals("editor", notification.getRole());
        assertFalse(notification.isRead());
        assertTrue(notification.getMessage().contains("Sistema"));
        assertTrue(notification.getMessage().contains("Ana Paz"));
    }

    @Test
    void unknownEmailIsRejected() {
        stubInviter();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        DomainException error = assertThrows(DomainException.class, () -> service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "nadie@x.com", "editor", "owner1")));

        assertEquals("El usuario no está registrado en UMLPilot", error.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void pendingInvitationCannotBeDuplicated() {
        stubInviter();
        when(userRepository.findByEmail("bob@x.com")).thenReturn(Optional.of(invitee));
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.of(
                Collaborator.reconstitute(new CollaboratorId("p1", "bob1"), "viewer", "pending", now, null)));

        DomainException error = assertThrows(DomainException.class, () -> service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "bob@x.com", "editor", "owner1")));

        assertEquals("El usuario ya tiene una invitación pendiente", error.getMessage());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void rejectedCollaboratorCanBeInvitedAgain() {
        stubInviter();
        when(userRepository.findByEmail("bob@x.com")).thenReturn(Optional.of(invitee));
        Collaborator rejected = Collaborator.reconstitute(
                new CollaboratorId("p1", "bob1"), "viewer", "rejected", now, null);
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1"))
                .thenReturn(Optional.of(rejected));

        CollaboratorResult result = service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "bob@x.com", "editor", "owner1"));

        assertEquals("pending", result.invitationStatus());
        assertEquals("editor", result.role());
        verify(collaboratorRepository).save(rejected);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void selfInvitationIsRejected() {
        when(projectRepository.findById(ProjectId.from("p1"))).thenReturn(Optional.of(project));
        when(collaboratorRepository.findAcceptedCollaborator("p1", "owner1"))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", "owner1", "owner")));
        when(userRepository.findByEmail("ana@x.com")).thenReturn(Optional.of(inviter));

        DomainException error = assertThrows(DomainException.class, () -> service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "ana@x.com", "editor", "owner1")));

        assertEquals("No puedes invitarte a ti mismo", error.getMessage());
    }

    @Test
    void acceptingInvitationMarksNotificationAsRead() {
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.of(
                Collaborator.reconstitute(new CollaboratorId("p1", "bob1"), "editor", "pending", now, null)));

        service.acceptInvitation("p1", "bob1");

        verify(collaboratorRepository).save(any(Collaborator.class));
        verify(notificationRepository).markInvitationAsRead("bob1", "p1");
    }
}
