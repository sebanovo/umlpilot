package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.application.dto.*;
import com.umlpilot.umlpilot_api.application.service.ElementService;
import com.umlpilot.umlpilot_api.application.service.RelationshipService;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import com.umlpilot.umlpilot_api.infrastructure.websocket.DiagramEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UmlToolsBehaviorTest {

    private ElementService elementService;
    private RelationshipService relationshipService;
    private DiagramEventPublisher publisher;
    private UmlTools tools;
    private ActionRecorder recorder;

    private static final String DIAGRAM = "d1";
    private static final String USER = "u1";

    @BeforeEach
    void setUp() {
        elementService = mock(ElementService.class);
        relationshipService = mock(RelationshipService.class);
        publisher = mock(DiagramEventPublisher.class);
        tools = new UmlTools(elementService, relationshipService, publisher);
        recorder = new ActionRecorder();
    }

    private ToolContext ctx() {
        Map<String, Object> context = new HashMap<>();
        context.put(UmlTools.CTX_DIAGRAM_ID, DIAGRAM);
        context.put(UmlTools.CTX_USER_ID, USER);
        context.put(UmlTools.CTX_ACTIONS, recorder);
        return new ToolContext(context);
    }

    private ElementResult element(String id, String name) {
        return new ElementResult(id, DIAGRAM, name, "public", null, "class", 0, 0, 100, 80);
    }

    private RelationshipResult relationship(String id) {
        return new RelationshipResult(id, DIAGRAM, "a", "b", "association", "", "bidirectional", null, null, null);
    }

    @Test
    void createElementPersistsAndPublishesEvent() {
        when(elementService.createElement(any())).thenReturn(element("e1", "Cliente"));

        tools.createElement("class", "Cliente", null, null, ctx());

        verify(elementService).createElement(argThat(c ->
                DIAGRAM.equals(c.diagramId()) && USER.equals(c.creatorId()) && "Cliente".equals(c.name())));
        verify(publisher).elementCreated(DIAGRAM, USER, "e1");
        assertTrue(recorder.actions().stream().anyMatch(a -> "element".equals(a.get("entity"))));
    }

    @Test
    void deleteElementValidatesDiagramAndPublishesEvent() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("e1", "Cliente")));

        tools.deleteElement("e1", ctx());

        verify(elementService).deleteElement("e1");
        verify(publisher).elementDeleted(DIAGRAM, USER, "e1");
    }

    @Test
    void deleteElementRejectsElementFromAnotherDiagram() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("e1", "Cliente")));

        assertThrows(DomainException.class, () -> tools.deleteElement("ghost", ctx()));
        verify(elementService, never()).deleteElement(anyString());
        verifyNoInteractions(publisher);
    }

    @Test
    void createRelationshipPersistsAndPublishesEvent() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("a", "A"), element("b", "B")));
        when(relationshipService.createRelationship(any())).thenReturn(relationship("r1"));

        tools.createRelationship("a", "b", "association", null, ctx());

        verify(relationshipService).createRelationship(argThat(c ->
                "a".equals(c.sourceElementId()) && "b".equals(c.targetElementId()) && "association".equals(c.relationshipType())));
        verify(publisher).relationshipCreated(DIAGRAM, USER, "r1");
    }

    @Test
    void createRelationshipRejectsEndpointOutsideDiagram() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("a", "A")));

        assertThrows(DomainException.class, () -> tools.createRelationship("a", "ghost", "association", null, ctx()));
        verify(relationshipService, never()).createRelationship(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void deleteRelationshipValidatesDiagramAndPublishesEvent() {
        when(relationshipService.getRelationships(DIAGRAM)).thenReturn(List.of(relationship("r1")));

        tools.deleteRelationship("r1", ctx());

        verify(relationshipService).deleteRelationship("r1");
        verify(publisher).relationshipDeleted(DIAGRAM, USER, "r1");
    }

    @Test
    void addAttributeDelegatesAndNotifiesElementUpdate() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("a", "A")));
        when(elementService.getAttributes("a")).thenReturn(List.of());
        when(elementService.addAttribute(eq("a"), eq("nombre"), eq("String"), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(String.class), anyInt()))
                .thenReturn(new AttributeResult("at1", "a", "nombre", "String", "private", null, null, null, false, false, 0));

        tools.addAttribute("a", "nombre", "String", null, null, null, null, ctx());

        verify(publisher).elementUpdated(DIAGRAM, USER, "a");
    }

    @Test
    void addMethodDelegatesAndNotifiesElementUpdate() {
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("a", "A")));
        when(elementService.getMethods("a")).thenReturn(List.of());
        when(elementService.addMethod(eq("a"), eq("calcular"), eq("int"), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(String.class), anyInt()))
                .thenReturn(new MethodResult("m1", "a", "calcular", "int", "public", null, null, null, false, false, false, false, 0, List.of()));

        tools.addMethod("a", "calcular", "int", null, null, null, null, ctx());

        verify(publisher).elementUpdated(DIAGRAM, USER, "a");
    }

    @Test
    void updateAttributeResolvesElementAndPublishesEvent() {
        AttributeResult attr = new AttributeResult("at1", "a", "nombre", "String", "private", null, null, null, false, false, 0);
        when(elementService.getElements(DIAGRAM)).thenReturn(List.of(element("a", "A")));
        when(elementService.getAttributes("a")).thenReturn(List.of(attr));
        when(elementService.updateAttribute(eq("at1"), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(String.class)))
                .thenReturn(attr);

        tools.updateAttribute("at1", "nuevo", null, null, null, null, null, ctx());

        verify(publisher).elementUpdated(DIAGRAM, USER, "a");
    }

    @Test
    void setMultiplicityUpdatesAndPublishesRelationshipUpdate() {
        when(relationshipService.getRelationships(DIAGRAM)).thenReturn(List.of(relationship("r1")));
        when(relationshipService.getMultiplicities("r1"))
                .thenReturn(List.of(new MultiplicityResult("m1", "r1", "source", 1, 1, false, false)));

        tools.setMultiplicity("r1", "source", 0, -1, ctx());

        verify(relationshipService).updateMultiplicity("m1", 0, -1);
        verify(publisher).relationshipUpdated(DIAGRAM, USER, "r1");
    }
}
