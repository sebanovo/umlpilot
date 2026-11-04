package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.InviteCollaboratorCommand;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CollaboratorServiceRoleTest {

    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CollaboratorService service = new CollaboratorService(
            collaboratorRepository, projectRepository, userRepository, notificationRepository);

    private final LocalDateTime now = LocalDateTime.now();

    private Collaborator member(String role) {
        return Collaborator.reconstitute(new CollaboratorId("p1", "bob1"), role, "accepted", now, now);
    }

    private void stubRequester(String userId, String role) {
        when(collaboratorRepository.findAcceptedCollaborator("p1", userId))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", userId, role)));
    }

    @Test
    void ownerCanChangeMemberRole() {
        stubRequester("owner1", "owner");
        Collaborator target = member("editor");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.of(target));

        service.changeRole("p1", "bob1", "viewer", "owner1");

        assertEquals("viewer", target.getRole());
        verify(collaboratorRepository).save(target);
    }

    @Test
    void editorCannotChangeRoles() {
        stubRequester("ed1", "editor");

        DomainException error = assertThrows(DomainException.class,
                () -> service.changeRole("p1", "bob1", "viewer", "ed1"));

        assertEquals("Solo el owner puede cambiar roles", error.getMessage());
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void ownerRoleCannotBeChanged() {
        stubRequester("owner1", "owner");
        Collaborator owner = Collaborator.createAccepted("p1", "owner1", "owner");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "owner1")).thenReturn(Optional.of(owner));

        DomainException error = assertThrows(DomainException.class,
                () -> service.changeRole("p1", "owner1", "viewer", "owner1"));

        assertEquals("El rol del owner no se puede modificar", error.getMessage());
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void invalidRoleIsRejected() {
        stubRequester("owner1", "owner");
        Collaborator target = member("editor");
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "bob1")).thenReturn(Optional.of(target));

        assertThrows(DomainException.class, () -> service.changeRole("p1", "bob1", "owner", "owner1"));
        assertThrows(DomainException.class, () -> service.changeRole("p1", "bob1", null, "owner1"));
        assertEquals("editor", target.getRole());
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void inviteCannotAssignOwnerRole() {
        DomainException error = assertThrows(DomainException.class, () -> service.inviteCollaborator(
                new InviteCollaboratorCommand("p1", "bob@x.com", "owner", "owner1")));

        assertEquals("Rol inválido. Usa 'editor' o 'viewer'", error.getMessage());
        verifyNoInteractions(projectRepository, userRepository, notificationRepository);
    }
}
