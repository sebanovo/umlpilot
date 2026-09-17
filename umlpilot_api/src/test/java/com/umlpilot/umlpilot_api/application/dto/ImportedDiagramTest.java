package com.umlpilot.umlpilot_api.application.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ImportedDiagramTest {

    @Test
    void buildsAnImportedDiagramFromRecords() {
        ImportedDiagram.AttributeSpec attribute =
                new ImportedDiagram.AttributeSpec("nombre", "String", "private", null);
        ImportedDiagram.MethodSpec method = new ImportedDiagram.MethodSpec(
                "saludar", "String", "public",
                List.of(new ImportedDiagram.ParameterSpec("veces", "int", "in")));
        ImportedDiagram.ElementSpec element = new ImportedDiagram.ElementSpec(
                "c1", "class", "Cliente", "public", 10, 20, 200, 120,
                List.of(attribute), List.of(method), List.of());
        ImportedDiagram.RelationshipSpec relationship = new ImportedDiagram.RelationshipSpec(
                "association", "c1", "c2", "compra", "1", "0..*", null);

        ImportedDiagram diagram = new ImportedDiagram(List.of(element), List.of(relationship), List.of("w"));

        assertEquals(1, diagram.elements().size());
        assertEquals("Cliente", diagram.elements().get(0).name());
        assertEquals(10, diagram.elements().get(0).positionX());
        assertEquals("0..*", diagram.relationships().get(0).targetMultiplicity());
        assertEquals(List.of("w"), diagram.warnings());
        assertNull(diagram.relationships().get(0).associationClassKey());
    }
}
