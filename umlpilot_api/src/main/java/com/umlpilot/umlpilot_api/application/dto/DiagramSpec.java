package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/**
 * Especificación de un diagrama UML generada por la IA (imagen o texto). Usa claves
 * locales ({@code key}) para que las relaciones no dependan de ids de base de datos.
 */
public record DiagramSpec(
        String title,
        List<ElementSpec> elements,
        List<RelationshipSpec> relationships) {

    public record ElementSpec(
            String key,
            String elementType,
            String name,
            List<AttributeSpec> attributes,
            List<MethodSpec> methods) {
    }

    public record AttributeSpec(
            String name,
            String dataType,
            String visibility,
            String alias,
            String stereotype,
            String defaultValue) {
    }

    public record MethodSpec(
            String name,
            String returnType,
            String visibility,
            String alias,
            String stereotype,
            String defaultValue,
            List<ParameterSpec> parameters) {
    }

    public record ParameterSpec(
            String name,
            String dataType,
            String direction,
            String notes) {
    }

    public record RelationshipSpec(
            String type,
            String sourceKey,
            String targetKey,
            String name,
            String sourceMultiplicity,
            String targetMultiplicity) {
    }
}
