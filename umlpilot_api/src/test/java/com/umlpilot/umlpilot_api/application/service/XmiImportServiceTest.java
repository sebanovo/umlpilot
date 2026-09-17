package com.umlpilot.umlpilot_api.application.service;

import com.umlpilot.umlpilot_api.application.dto.CreateElementCommand;
import com.umlpilot.umlpilot_api.application.dto.ElementResult;
import com.umlpilot.umlpilot_api.application.dto.ImportXmiResult;
import com.umlpilot.umlpilot_api.application.dto.ImportedDiagram;
import com.umlpilot.umlpilot_api.application.dto.MethodResult;
import com.umlpilot.umlpilot_api.application.dto.MultiplicityResult;
import com.umlpilot.umlpilot_api.application.dto.RelationshipResult;
import com.umlpilot.umlpilot_api.application.port.out.DiagramChangeNotifier;
import com.umlpilot.umlpilot_api.application.port.out.XmiDocumentParser;
import com.umlpilot.umlpilot_api.domain.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class XmiImportServiceTest {

    private final XmiDocumentParser parser = mock(XmiDocumentParser.class);
    private final ElementService elementService = mock(ElementService.class);
    private final RelationshipService relationshipService = mock(RelationshipService.class);
    private final DiagramChangeNotifier changeNotifier = mock(DiagramChangeNotifier.class);
    private final XmiImportService service =
            new XmiImportService(parser, elementService, relationshipService, changeNotifier);

    private void stubElementCreation() {
        AtomicInteger counter = new AtomicInteger();
        when(elementService.createElement(any(CreateElementCommand.class))).thenAnswer(invocation -> {
            CreateElementCommand command = invocation.getArgument(0);
            String id = "id-" + counter.incrementAndGet();
            return new ElementResult(id, command.diagramId(), command.name(), "public", null,
                    command.elementType(), command.positionX(), command.positionY(), 100, 80);
        });
        when(elementService.addMethod(anyString(), anyString(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new MethodResult("m-1", "id-1", "m", "void", "public", null, null, null,
                        false, false, false, false, 0, List.of()));
        when(relationshipService.createRelationship(any()))
                .thenReturn(new RelationshipResult("r-1", "d1", "id-1", "id-2", "association", null,
                        "bidirectional", null, null, null));
        when(relationshipService.updateRelationship(anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new RelationshipResult("r-1", "d1", "id-1", "id-2", "association", "compra",
                        "bidirectional", null, null, null));
        when(relationshipService.getMultiplicities(anyString())).thenReturn(List.of(
                new MultiplicityResult("mu-s", "r-1", "source", null, null, false, false),
                new MultiplicityResult("mu-t", "r-1", "target", null, null, false, false)));
    }

    private static ImportedDiagram.ElementSpec element(String key, String name, String type,
                                                       Integer x, Integer y) {
        return new ImportedDiagram.ElementSpec(key, type, name, "public", x, y, 200, 120,
                List.of(), List.of(), List.of());
    }

    // 2.1 aditiva

    @Test
    void importsAdditivelyWithoutTouchingExistingContent() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of(
                new ElementResult("exist-1", "d1", "Existente", "public", null, "class", 0, 0, 100, 80)));
        when(parser.parse(any())).thenReturn(new ImportedDiagram(
                List.of(element("c1", "Cliente", "class", 10, 20), element("c2", "Pedido", "class", 300, 20)),
                List.of(new ImportedDiagram.RelationshipSpec("association", "c1", "c2", "compra", "1", "0..*", null)),
                List.of()));

        ImportXmiResult result = service.importDiagram("d1", "u1", new byte[] {1});

        assertEquals(2, result.elements());
        assertEquals(1, result.relationships());
        verify(elementService, times(2)).createElement(any());
        verify(elementService, never()).deleteElement(any());
        verify(relationshipService, times(1)).createRelationship(any());
    }

    // 2.2 renombrado por colisión

    @Test
    void renamesCollidingNamesWithNumericSuffixAndWarns() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of(
                new ElementResult("exist-1", "d1", "Cliente", "public", null, "class", 0, 0, 100, 80)));
        when(parser.parse(any())).thenReturn(new ImportedDiagram(
                List.of(element("c1", "Cliente", "class", null, null),
                        element("c2", "Cliente", "class", null, null)),
                List.of(), List.of()));

        ImportXmiResult result = service.importDiagram("d1", "u1", new byte[] {1});

        ArgumentCaptor<CreateElementCommand> captor = ArgumentCaptor.forClass(CreateElementCommand.class);
        verify(elementService, times(2)).createElement(captor.capture());
        assertEquals("Cliente_1", captor.getAllValues().get(0).name());
        assertEquals("Cliente_2", captor.getAllValues().get(1).name());
        assertEquals(2, result.warnings().stream().filter(w -> w.contains("colisión")).count());
    }

    // 2.3 posiciones

    @Test
    void keepsBoundsAndFallsBackToGridBelowExistingContent() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of(
                new ElementResult("exist-1", "d1", "Existente", "public", null, "class", 0, 100, 100, 80)));
        when(parser.parse(any())).thenReturn(new ImportedDiagram(
                List.of(element("c1", "ConBounds", "class", 500, 300),
                        element("c2", "SinBounds", "class", null, null)),
                List.of(), List.of()));

        service.importDiagram("d1", "u1", new byte[] {1});

        ArgumentCaptor<CreateElementCommand> captor = ArgumentCaptor.forClass(CreateElementCommand.class);
        verify(elementService, times(2)).createElement(captor.capture());
        assertEquals(500, captor.getAllValues().get(0).positionX());
        assertEquals(300, captor.getAllValues().get(0).positionY());
        // el segundo elemento (sin bounds) cae en la cuadrícula bajo el contenido existente
        assertEquals(40 + 220, captor.getAllValues().get(1).positionX()); // columna 1 de 2
        assertEquals(220, captor.getAllValues().get(1).positionY()); // 100 + 80 + 40
    }

    // 2.4 tope de elementos

    @Test
    void rejectsMoreThanTwoHundredElements() {
        List<ImportedDiagram.ElementSpec> elements = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            elements.add(element("c" + i, "Clase" + i, "class", null, null));
        }
        when(parser.parse(any())).thenReturn(new ImportedDiagram(elements, List.of(), List.of()));

        DomainException error = assertThrows(DomainException.class,
                () -> service.importDiagram("d1", "u1", new byte[] {1}));
        assertTrue(error.getMessage().contains("200"), error.getMessage());
        verify(elementService, never()).createElement(any());
    }

    // 2.5 evento único

    @Test
    void publishesExactlyOneBulkChangeEventWithImportOrigin() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of());
        when(parser.parse(any())).thenReturn(new ImportedDiagram(
                List.of(element("c1", "Cliente", "class", null, null)), List.of(), List.of()));

        service.importDiagram("d1", "u1", new byte[] {1});

        verify(changeNotifier, times(1)).bulkChanged("d1", "u1", "import");
    }

    // atributos/parámetros sin tipo usan el default de la UI ("String")

    @Test
    void defaultsMissingTypesToTheUiDefault() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of());
        ImportedDiagram.ElementSpec rich = new ImportedDiagram.ElementSpec(
                "c1", "class", "Cliente", "public", null, null, null, null,
                List.of(new ImportedDiagram.AttributeSpec("monto_total", null, "private", null)),
                List.of(new ImportedDiagram.MethodSpec("calcular", "void", "public",
                        List.of(new ImportedDiagram.ParameterSpec("factor", null, "in")))),
                List.of());
        when(parser.parse(any())).thenReturn(new ImportedDiagram(List.of(rich), List.of(), List.of()));

        service.importDiagram("d1", "u1", new byte[] {1});

        ArgumentCaptor<String> attributeType = ArgumentCaptor.forClass(String.class);
        verify(elementService).addAttribute(anyString(), eq("monto_total"), attributeType.capture(),
                any(), any(), any(), any(), any());
        assertEquals("String", attributeType.getValue());

        ArgumentCaptor<String> parameterType = ArgumentCaptor.forClass(String.class);
        verify(elementService).addParameter(anyString(), eq("factor"), parameterType.capture(),
                any(), any(), any());
        assertEquals("String", parameterType.getValue());
    }

    // 5.2 un rechazo no modifica el diagrama
    @Test
    void rejectedImportDoesNotTouchTheDiagram() {
        when(parser.parse(any())).thenThrow(new DomainException(
                "Versión XMI no soportada. Se requiere XMI 2.5.1. Versión detectada: 2.1"));

        assertThrows(DomainException.class, () -> service.importDiagram("d1", "u1", new byte[] {1}));

        verify(elementService, never()).createElement(any());
        verify(relationshipService, never()).createRelationship(any());
        verify(elementService, never()).deleteElement(any());
        verifyNoInteractions(changeNotifier);
    }

    // conteos de atributos/métodos/parámetros/literales
    @Test
    void countsImportedChildren() {
        stubElementCreation();
        when(elementService.getElements("d1")).thenReturn(List.of());
        ImportedDiagram.ElementSpec rich = new ImportedDiagram.ElementSpec(
                "c1", "class", "Cliente", "public", null, null, null, null,
                List.of(new ImportedDiagram.AttributeSpec("nombre", "String", "private", null)),
                List.of(new ImportedDiagram.MethodSpec("saludar", "String", "public",
                        List.of(new ImportedDiagram.ParameterSpec("veces", "int", "in")))),
                List.of(new ImportedDiagram.LiteralSpec("ACTIVO", "1")));
        when(parser.parse(any())).thenReturn(new ImportedDiagram(List.of(rich), List.of(), List.of()));

        ImportXmiResult result = service.importDiagram("d1", "u1", new byte[] {1});

        assertEquals(1, result.elements());
        assertEquals(1, result.attributes());
        assertEquals(1, result.methods());
        assertEquals(1, result.parameters());
        assertEquals(1, result.literals());
    }
}
