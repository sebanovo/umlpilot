package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.CollaboratorId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.NotificationRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import com.umlpilot.umlpilot_api.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollaboratorServiceRemoveTest {

    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CollaboratorService service = new CollaboratorService(
            collaboratorRepository, projectRepository, userRepository, notificationRepository);

    private final LocalDateTime now = LocalDateTime.now();

    private void stubRequester(String userId, String role) {
        when(collaboratorRepository.findAcceptedCollaborator("p1", userId))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", userId, role)));
    }

    private Collaborator collaborator(String userId, String role) {
        return Collaborator.reconstitute(new CollaboratorId("p1", userId), role, "accepted", now, now);
    }

    @Test
    void ownerRemovesMember() {
        stubRequester("owner1", "owner");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1"))
                .thenReturn(Optional.of(collaborator("bob1", "editor")));

        service.removeCollaborator("p1", "bob1", "owner1");

        verify(collaboratorRepository).delete(new CollaboratorId("p1", "bob1"));
    }

    @Test
    void removingPendingInvitationDeletesNotification() {
        stubRequester("owner1", "owner");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1"))
                .thenReturn(Optional.of(Collaborator.reconstitute(
                        new CollaboratorId("p1", "bob1"), "editor", "pending", now, null)));

        service.removeCollaborator("p1", "bob1", "owner1");

        verify(notificationRepository).deleteInvitation("bob1", "p1");
        verify(collaboratorRepository).delete(new CollaboratorId("p1", "bob1"));
    }

    @Test
    void ownerCannotRemoveHimself() {
        stubRequester("owner1", "owner");

        DomainException error = assertThrows(DomainException.class,
                () -> service.removeCollaborator("p1", "owner1", "owner1"));

        assertEquals("El owner no puede eliminarse a sí mismo", error.getMessage());
        verify(collaboratorRepository, never()).delete(any());
    }

    @Test
    void editorCannotRemoveAnotherMember() {
        stubRequester("ed1", "editor");

        DomainException error = assertThrows(DomainException.class,
                () -> service.removeCollaborator("p1", "bob1", "ed1"));

        assertEquals("Sin permisos para eliminar a otro colaborador", error.getMessage());
        verify(collaboratorRepository, never()).delete(any());
    }

    @Test
    void editorCanRemoveHimself() {
        stubRequester("ed1", "editor");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "ed1"))
                .thenReturn(Optional.of(collaborator("ed1", "editor")));

        service.removeCollaborator("p1", "ed1", "ed1");

        verify(collaboratorRepository).delete(new CollaboratorId("p1", "ed1"));
    }
}
