package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/**
 * Modelo intermedio ya parseado y validado de un archivo XMI de clases, listo para
 * persistirse de forma aditiva sobre un diagrama existente.
 */
public record ImportedDiagram(
        List<ElementSpec> elements,
        List<RelationshipSpec> relationships,
        List<String> warnings) {

    public record ElementSpec(
            String localKey,
            String elementType,
            String name,
            String visibility,
            Integer positionX,
            Integer positionY,
            Integer width,
            Integer height,
            List<AttributeSpec> attributes,
            List<MethodSpec> methods,
            List<LiteralSpec> literals) {
    }

    public record AttributeSpec(String name, String dataType, String visibility, String defaultValue) {
    }

    public record MethodSpec(String name, String returnType, String visibility, List<ParameterSpec> parameters) {
    }

    public record ParameterSpec(String name, String dataType, String direction) {
    }

    public record LiteralSpec(String name, String value) {
    }

    public record Bounds(Integer x, Integer y, Integer width, Integer height) {
    }

    public record RelationshipSpec(
            String type,
            String sourceKey,
            String targetKey,
            String name,
            String sourceMultiplicity,
            String targetMultiplicity,
            String associationClassKey) {
    }
}
