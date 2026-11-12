package com.umlpilot.umlpilot_api.infrastructure.xmi;

import com.umlpilot.umlpilot_api.application.dto.XmiExportModel;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EaXmiRendererTest {

    private final EaXmiRenderer renderer = new EaXmiRenderer();

    @Test
    void rendersEaCompatibleHeaderAndDiagramGeometry() {
        String diagramId = "c746abf9-7aa1-423d-813e-f9515e4b22d1";
        XmiExportModel.Element cliente = new XmiExportModel.Element(
                "e1", "DUEÑO", "class", null, "public", 70, 90, 180, 97,
                List.of(new XmiExportModel.Attribute("a1", "id", "int", "private", null, null, false)),
                List.of());
        XmiExportModel.Element pedido = new XmiExportModel.Element(
                "e2", "PEDIDO", "class", null, "public", 435, 75, 180, 113,
                List.of(new XmiExportModel.Attribute("a2", "id", "int", "private", null, null, false)),
                List.of());
        XmiExportModel.Relationship relationship = new XmiExportModel.Relationship(
                "r1", "association", "hace", "e1", "e2", "1", "0..*", null, null);
        XmiExportModel model = new XmiExportModel(
                diagramId, "Diagram_" + diagramId, List.of(cliente, pedido), List.of(relationship));

        byte[] bytes = renderer.render(model);
        String xml = new String(bytes, Charset.forName("windows-1252"));

        assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"windows-1252\"?>"));
        assertTrue(xml.contains("xmlns:thecustomprofile=\"http://www.sparxsystems.com/profiles/thecustomprofile/1.0\""));
        assertTrue(xml.contains("<xmi:Documentation exporter=\"Enterprise Architect\" exporterVersion=\"6.5\"/>"));
        assertTrue(xml.contains("<umldi:Diagram xmi:type=\"umldi:UMLClassDiagram\""));
        assertTrue(xml.contains("geometry=\"Left=70;Top=90;Right=250;Bottom=187;\""));
        assertTrue(xml.contains("<diagram xmi:id=\"EAID_"));
        assertFalse(xml.contains("xmi:version="));
        assertFalse(xml.contains("_undefined"));

        Matcher shape = Pattern.compile("xmi:type=\"umldi:UMLClassifierShape\" xmi:id=\"([^\"]+)\"").matcher(xml);
        assertTrue(shape.find());
        assertTrue(shape.group(1).matches("EAID_[0-9A-F]{8}(_[0-9A-F]{4}){3}_[0-9A-F]{12}"), shape.group(1));
        assertTrue(xml.contains("DUEÑO"));
    }
}
