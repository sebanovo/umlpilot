package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/**
 * Resultado de construir un diagrama a partir de una {@link DiagramSpec}.
 */
public record DiagramBuildResult(
        int elements,
        int attributes,
        int methods,
        int parameters,
        int relationships,
        List<String> elementIds,
        List<String> relationshipIds) {

    public String summary() {
        return elements + " elemento(s), " + attributes + " atributo(s), " + methods + " método(s)"
                + (parameters > 0 ? ", " + parameters + " parámetro(s)" : "")
                + " y " + relationships + " relación(es)";
    }
}
