package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.CollaboratorId;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.UserId;
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

class CollaboratorServiceJoinTest {

    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final NotificationRepository notificationRepository = mock(NotificationRepository.class);
    private final CollaboratorService service = new CollaboratorService(
            collaboratorRepository, projectRepository, userRepository, notificationRepository);

    private final LocalDateTime now = LocalDateTime.now();
    private final Project project = Project.reconstitute(ProjectId.from("p1"), "Proyecto", null,
            UserId.from("creator"), "active", now, now);

    private void stubProject() {
        when(projectRepository.findById(ProjectId.from("p1"))).thenReturn(Optional.of(project));
    }

    @Test
    void newUserJoinsWithLinkRole() {
        stubProject();
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "u2")).thenReturn(Optional.empty());

        String role = service.joinByLink("p1", "u2", "editor");

        assertEquals("editor", role);
        verify(collaboratorRepository).save(any(Collaborator.class));
    }

    @Test
    void acceptedMemberKeepsRole() {
        stubProject();
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "u2")).thenReturn(Optional.of(
                Collaborator.createAccepted("p1", "u2", "viewer")));

        String role = service.joinByLink("p1", "u2", "editor");

        assertEquals("viewer", role);
        verify(collaboratorRepository, never()).save(any());
    }

    @Test
    void pendingInvitationIsAccepted() {
        stubProject();
        Collaborator pending = Collaborator.reconstitute(
                new CollaboratorId("p1", "u2"), "editor", "pending", now, null);
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "u2")).thenReturn(Optional.of(pending));

        String role = service.joinByLink("p1", "u2", "viewer");

        assertEquals("editor", role);
        assertEquals("accepted", pending.getInvitationStatus());
        verify(collaboratorRepository).save(pending);
        verify(notificationRepository).markInvitationAsRead("u2", "p1");
    }

    @Test
    void rejectedUserRejoinsWithLinkRole() {
        stubProject();
        Collaborator rejected = Collaborator.reconstitute(
                new CollaboratorId("p1", "u2"), "viewer", "rejected", now, null);
        when(collaboratorRepository.findByProjectIdAndUserId("p1", "u2")).thenReturn(Optional.of(rejected));

        String role = service.joinByLink("p1", "u2", "editor");

        assertEquals("editor", role);
        assertEquals("accepted", rejected.getInvitationStatus());
        verify(collaboratorRepository).save(rejected);
    }

    @Test
    void invalidLinkRoleIsRejected() {
        assertThrows(DomainException.class, () -> service.joinByLink("p1", "u2", "owner"));
        verifyNoInteractions(projectRepository, collaboratorRepository);
    }
}
