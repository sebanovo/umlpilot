package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.domain.model.Element;
import com.umlpilot.umlpilot_api.domain.model.ElementId;
import com.umlpilot.umlpilot_api.domain.repository.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ElementServiceDeleteAllTest {

    private final ElementRepository elementRepository = mock(ElementRepository.class);
    private final AttributeRepository attributeRepository = mock(AttributeRepository.class);
    private final MethodRepository methodRepository = mock(MethodRepository.class);
    private final ParameterRepository parameterRepository = mock(ParameterRepository.class);
    private final LiteralRepository literalRepository = mock(LiteralRepository.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final ElementService service = new ElementService(elementRepository, attributeRepository,
            methodRepository, parameterRepository, literalRepository, relationshipService);

    @Test
    void deletesRelationshipsAndElementsOfTheDiagram() {
        when(relationshipService.getRelationships("d1")).thenReturn(List.of(
                new RelationshipResult("r1", "d1", "e1", "e2", "association", null, "bidirectional",
                        null, null, null)));
        Element element = mock(Element.class);
        when(element.getId()).thenReturn(ElementId.generate());
        when(elementRepository.findByDiagramId("d1")).thenReturn(List.of(element));
        when(attributeRepository.findByElementId(anyString())).thenReturn(List.of());
        when(methodRepository.findByElementId(anyString())).thenReturn(List.of());
        when(literalRepository.findByElementId(anyString())).thenReturn(List.of());

        int deleted = service.deleteAllElements("d1");

        assertEquals(1, deleted);
        verify(relationshipService, times(1)).deleteRelationship("r1");
        verify(elementRepository, times(1)).delete(any());
    }

    @Test
    void emptyDiagramDeletesNothing() {
        when(relationshipService.getRelationships("d1")).thenReturn(List.of());
        when(elementRepository.findByDiagramId("d1")).thenReturn(List.of());

        assertEquals(0, service.deleteAllElements("d1"));
        verify(elementRepository, never()).delete(any());
    }
}
