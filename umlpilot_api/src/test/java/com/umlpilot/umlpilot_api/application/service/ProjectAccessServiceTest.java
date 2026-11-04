package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Collaborator;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.CollaboratorRepository;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProjectAccessServiceTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final CollaboratorRepository collaboratorRepository = mock(CollaboratorRepository.class);
    private final DiagramRepository diagramRepository = mock(DiagramRepository.class);
    private final ProjectAccessService service = new ProjectAccessService(
            projectRepository, collaboratorRepository, diagramRepository);

    private final LocalDateTime now = LocalDateTime.now();
    private final Project project = Project.reconstitute(ProjectId.from("p1"), "Proyecto", null,
            UserId.from("creator"), "active", now, now);

    private void stubProject() {
        when(projectRepository.findById(ProjectId.from("p1"))).thenReturn(Optional.of(project));
    }

    @Test
    void creatorCanReadAndEdit() {
        stubProject();

        assertSame(project, service.requireMember("p1", "creator"));
        assertDoesNotThrow(() -> service.requireEditor("p1", "creator"));
        assertDoesNotThrow(() -> service.requireOwner("p1", "creator"));
    }

    @Test
    void viewerCanReadButNotEdit() {
        stubProject();
        when(collaboratorRepository.findAcceptedCollaborator("p1", "viewer"))
                .thenReturn(Optional.of(Collaborator.createAccepted("p1", "viewer", "viewer")));

        assertDoesNotThrow(() -> service.requireMember("p1", "viewer"));

        DomainException error = assertThrows(DomainException.class,
                () -> service.requireEditor("p1", "viewer"));
        assertEquals("No tienes permisos para editar este proyecto", error.getMessage());
    }

    @Test
    void removedUserHasNoAccess() {
        stubProject();
        when(collaboratorRepository.findAcceptedCollaborator("p1", "removed")).thenReturn(Optional.empty());

        DomainException error = assertThrows(DomainException.class,
                () -> service.requireMember("p1", "removed"));
        assertEquals("No tienes acceso a este proyecto", error.getMessage());
    }

    @Test
    void anonymousUserHasNoAccess() {
        assertThrows(DomainException.class, () -> service.requireMember("p1", null));
    }

    @Test
    void diagramMustBelongToProject() {
        stubProject();
        Diagram other = Diagram.reconstitute(DiagramId.from("d2"), ProjectId.from("p2"), "Otro", "class",
                null, null, 1, false, null, now, now);
        when(diagramRepository.findById(DiagramId.from("d2"))).thenReturn(Optional.of(other));

        DomainException error = assertThrows(DomainException.class,
                () -> service.requireDiagramMember("p1", "d2", "creator"));
        assertEquals("El diagrama no pertenece al proyecto", error.getMessage());
    }

    @Test
    void diagramAccessRequiresMembership() {
        when(diagramRepository.findById(DiagramId.from("d1"))).thenReturn(Optional.of(
                Diagram.reconstitute(DiagramId.from("d1"), ProjectId.from("p1"), "D", "class",
                        null, null, 1, false, null, now, now)));
        stubProject();
        when(collaboratorRepository.findAcceptedCollaborator("p1", "removed")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> service.requireDiagramMember("p1", "d1", "removed"));
    }
}
