package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/**
 * Instantánea de solo lectura de un diagrama completo: elementos (con atributos y
 * métodos/parámetros) y relaciones (con los extremos y sus multiplicidades).
 * Se usa como entrada de la generación de código.
 */
public record DiagramSnapshot(List<ElementSnapshot> elements, List<RelationshipSnapshot> relationships) {

    public record ElementSnapshot(
            String id,
            String name,
            String elementType,
            String stereotype,
            String visibility,
            List<AttributeSnapshot> attributes,
            List<MethodSnapshot> methods) {
    }

    public record AttributeSnapshot(
            String id,
            String name,
            String dataType,
            String visibility,
            String stereotype,
            String defaultValue) {
    }

    public record MethodSnapshot(
            String id,
            String name,
            String returnType,
            String visibility,
            List<ParameterSnapshot> parameters) {
    }

    public record ParameterSnapshot(
            String id,
            String name,
            String dataType,
            String direction) {
    }

    public record RelationshipSnapshot(
            String id,
            String type,
            String name,
            String sourceElementId,
            String targetElementId,
            String sourceMultiplicity,
            String targetMultiplicity,
            String associationClassElementId) {
    }
}
