package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.PublicDiagramView;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.application.dto.ShareJoinResult;
import com.umlpilot.umlpilot_api.application.dto.ShareLinkResult;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.domain.model.Diagram;
import com.umlpilot.umlpilot_api.domain.model.DiagramId;
import com.umlpilot.umlpilot_api.domain.model.DiagramShareLink;
import com.umlpilot.umlpilot_api.domain.model.Project;
import com.umlpilot.umlpilot_api.domain.model.ProjectId;
import com.umlpilot.umlpilot_api.domain.model.UserId;
import com.umlpilot.umlpilot_api.domain.repository.DiagramRepository;
import com.umlpilot.umlpilot_api.domain.repository.DiagramShareLinkRepository;
import com.umlpilot.umlpilot_api.domain.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DiagramShareServiceTest {

    private final DiagramShareLinkRepository shareLinkRepository = mock(DiagramShareLinkRepository.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final DiagramRepository diagramRepository = mock(DiagramRepository.class);
    private final ProjectAccessService accessService = mock(ProjectAccessService.class);
    private final CollaboratorService collaboratorService = mock(CollaboratorService.class);
    private final ElementService elementService = mock(ElementService.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final DiagramShareService service = new DiagramShareService(
            shareLinkRepository, projectRepository, diagramRepository, accessService,
            collaboratorService, elementService, relationshipService);

    private final LocalDateTime now = LocalDateTime.now();

    private DiagramShareLink link(boolean active, String role) {
        DiagramShareLink link = DiagramShareLink.create("p1", "d1", "u1", role);
        if (!active) link.revoke();
        return link;
    }

    private void stubSave() {
        when(shareLinkRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void enableCreatesActiveLinkWithRole() {
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.empty());
        stubSave();

        ShareLinkResult result = service.enable("p1", "d1", "u1", "editor");

        assertTrue(result.enabled());
        assertNotNull(result.token());
        assertEquals("/share/" + result.token(), result.url());
        assertEquals("editor", result.role());
        verify(accessService).requireDiagramEditor("p1", "d1", "u1");

        ArgumentCaptor<DiagramShareLink> captor = ArgumentCaptor.forClass(DiagramShareLink.class);
        verify(shareLinkRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive());
        assertEquals("editor", captor.getValue().getLinkRole());
    }

    @Test
    void enableDefaultsToViewer() {
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.empty());
        stubSave();

        ShareLinkResult result = service.enable("p1", "d1", "u1", null);

        assertEquals("viewer", result.role());
    }

    @Test
    void enableReusesAndUpdatesExistingLink() {
        DiagramShareLink existing = link(false, "viewer");
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.of(existing));
        stubSave();

        ShareLinkResult result = service.enable("p1", "d1", "u1", "editor");

        assertTrue(existing.isActive());
        assertEquals("editor", existing.getLinkRole());
        assertEquals("editor", result.role());
        assertEquals(existing.getToken(), result.token());
    }

    @Test
    void invalidRoleIsRejected() {
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> service.enable("p1", "d1", "u1", "owner"));
        verify(shareLinkRepository, never()).save(any());
    }

    @Test
    void disableRevokesLink() {
        DiagramShareLink existing = link(true, "viewer");
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.of(existing));

        service.disable("p1", "d1", "u1");

        assertFalse(existing.isActive());
        verify(shareLinkRepository).save(existing);
    }

    @Test
    void statusOfInactiveLinkKeepsRole() {
        when(shareLinkRepository.findByDiagramId("d1")).thenReturn(Optional.of(link(false, "editor")));

        ShareLinkResult result = service.getStatus("p1", "d1", "u1");

        assertFalse(result.enabled());
        assertNull(result.token());
        assertNull(result.url());
        assertEquals("editor", result.role());
    }

    @Test
    void joinAddsMemberWithLinkRole() {
        DiagramShareLink existing = link(true, "editor");
        when(shareLinkRepository.findActiveByToken(existing.getToken())).thenReturn(Optional.of(existing));
        when(collaboratorService.joinByLink("p1", "u2", "editor")).thenReturn("editor");

        ShareJoinResult result = service.join(existing.getToken(), "u2");

        assertEquals("p1", result.projectId());
        assertEquals("d1", result.diagramId());
        assertEquals("editor", result.role());
    }

    @Test
    void invalidTokenIsRejected() {
        when(shareLinkRepository.findActiveByToken("nope")).thenReturn(Optional.empty());

        DomainException error = assertThrows(DomainException.class, () -> service.getPublicView("nope"));
        assertEquals("El enlace no es válido o fue desactivado", error.getMessage());
    }

    @Test
    void publicViewAssemblesDiagramContent() {
        DiagramShareLink existing = link(true, "editor");
        when(shareLinkRepository.findActiveByToken(existing.getToken())).thenReturn(Optional.of(existing));
        when(projectRepository.findById(ProjectId.from("p1"))).thenReturn(Optional.of(
                Project.reconstitute(ProjectId.from("p1"), "Proyecto", null, UserId.from("creator"), "active", now, now)));
        when(diagramRepository.findById(DiagramId.from("d1"))).thenReturn(Optional.of(
                Diagram.reconstitute(DiagramId.from("d1"), ProjectId.from("p1"), "Clases", "class",
                        null, null, 1, false, null, now, now)));

        ElementResult element = new ElementResult("e1", "d1", "Cliente", "public", null, "class", 0, 0, 180, 80);
        RelationshipResult relationship = new RelationshipResult("r1", "d1", "e1", "e2", "association",
                null, "bidirectional", null, null, null);

        when(elementService.getElements("d1")).thenReturn(List.of(element));
        when(elementService.getAttributes("e1")).thenReturn(List.of());
        when(elementService.getMethods("e1")).thenReturn(List.of());
        when(elementService.getLiterals("e1")).thenReturn(List.of());
        when(relationshipService.getRelationships("d1")).thenReturn(List.of(relationship));
        when(relationshipService.getMultiplicities("r1")).thenReturn(List.of());

        PublicDiagramView view = service.getPublicView(existing.getToken());

        assertEquals("p1", view.projectId());
        assertEquals("d1", view.diagramId());
        assertEquals("editor", view.linkRole());
        assertEquals("Proyecto", view.projectName());
        assertEquals("Clases", view.diagramName());
        assertEquals(1, view.elements().size());
        assertEquals(1, view.relationships().size());
        assertTrue(view.attributes().containsKey("e1"));
        assertTrue(view.methods().containsKey("e1"));
        assertTrue(view.literals().containsKey("e1"));
        assertTrue(view.multiplicities().containsKey("r1"));
    }
}
