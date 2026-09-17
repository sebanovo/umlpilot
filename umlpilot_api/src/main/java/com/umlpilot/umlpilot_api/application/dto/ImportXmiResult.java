package com.umlpilot.umlpilot_api.application.dto;

import java.util.List;

/**
 * Resumen del resultado de importar un XMI: conteos de lo añadido y warnings
 * (renombrados por colisión, relaciones omitidas, textos truncados, etc.).
 */
public record ImportXmiResult(
        int elements,
        int attributes,
        int methods,
        int parameters,
        int literals,
        int relationships,
        List<String> warnings) {
}
