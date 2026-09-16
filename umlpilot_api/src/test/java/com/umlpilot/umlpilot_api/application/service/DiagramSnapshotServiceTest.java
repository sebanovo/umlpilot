package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiagramSnapshotServiceTest {

    private final ElementService elementService = mock(ElementService.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final DiagramSnapshotService service = new DiagramSnapshotService(elementService, relationshipService);

    @Test
    void composesElementsAttributesMethodsAndRelationships() {
        when(elementService.getElements("d1")).thenReturn(List.of(
                new ElementResult("e1", "d1", "Cliente", "public", null, "class", 0, 0, 100, 80),
                new ElementResult("e2", "d1", "Pedido", "public", null, "class", 200, 0, 100, 80)));
        when(elementService.getAttributes("e1")).thenReturn(List.of(
                new AttributeResult("a1", "e1", "nombre", "String", "private", null, null, null, false, false, 0)));
        when(elementService.getMethods("e1")).thenReturn(List.of());
        when(elementService.getAttributes("e2")).thenReturn(List.of());
        when(elementService.getMethods("e2")).thenReturn(List.of(
                new MethodResult("m1", "e2", "total", "double", "public", null, null, null,
                        false, false, false, false, 0,
                        List.of(new ParameterResult("p1", "m1", "iva", "double", "in", null, 0, false, false, null)))));

        when(relationshipService.getRelationships("d1")).thenReturn(List.of(
                new RelationshipResult("r1", "d1", "e1", "e2", "association", "compra", "bidirectional",
                        null, null, null)));
        when(relationshipService.getMultiplicities("r1")).thenReturn(List.of(
                new MultiplicityResult("mu1", "r1", "source", 1, 1, false, false),
                new MultiplicityResult("mu2", "r1", "target", 0, -1, false, false)));

        DiagramSnapshot snapshot = service.snapshot("d1");

        assertEquals(2, snapshot.elements().size());
        DiagramSnapshot.ElementSnapshot cliente = snapshot.elements().get(0);
        assertEquals("Cliente", cliente.name());
        assertEquals(1, cliente.attributes().size());
        assertEquals("nombre", cliente.attributes().get(0).name());

        DiagramSnapshot.ElementSnapshot pedido = snapshot.elements().get(1);
        assertEquals(1, pedido.methods().size());
        assertEquals(1, pedido.methods().get(0).parameters().size());
        assertEquals("iva", pedido.methods().get(0).parameters().get(0).name());

        assertEquals(1, snapshot.relationships().size());
        DiagramSnapshot.RelationshipSnapshot relationship = snapshot.relationships().get(0);
        assertEquals("1", relationship.sourceMultiplicity());
        assertEquals("0..*", relationship.targetMultiplicity());
    }

    @Test
    void formatsMultiplicityLabels() {
        assertEquals("1", DiagramSnapshotService.formatMultiplicity(new MultiplicityResult("m", "r", "source", 1, 1, false, false)));
        assertEquals("0..*", DiagramSnapshotService.formatMultiplicity(new MultiplicityResult("m", "r", "source", 0, -1, false, false)));
        assertEquals("1..*", DiagramSnapshotService.formatMultiplicity(new MultiplicityResult("m", "r", "source", 1, -1, false, false)));
        assertEquals("0..1", DiagramSnapshotService.formatMultiplicity(new MultiplicityResult("m", "r", "source", 0, 1, false, false)));
        assertNull(DiagramSnapshotService.formatMultiplicity(new MultiplicityResult("m", "r", "source", null, null, false, false)));
    }
}
