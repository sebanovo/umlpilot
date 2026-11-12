package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/** Modelo de solo lectura para renderizar un diagrama UML como XMI compatible con EA. */
public record XmiExportModel(String diagramId, String modelName, List<Element> elements, List<Relationship> relationships) {

    public record Element(
            String id, String name, String elementType, String stereotype, String visibility,
            int positionX, int positionY, int width, int height,
            List<Attribute> attributes, List<Method> methods) {
    }

    public record Attribute(
            String id, String name, String dataType, String visibility,
            String stereotype, String defaultValue, boolean isStatic) {
    }

    public record Method(
            String id, String name, String returnType, String visibility,
            String stereotype, boolean isStatic, List<Parameter> parameters) {
    }

    public record Parameter(String id, String name, String dataType, String direction, String notes) {
    }

    public record Relationship(
            String id, String type, String name, String sourceElementId, String targetElementId,
            String sourceMultiplicity, String targetMultiplicity, String associationClassElementId,
            String waypoints) {
    }
}
