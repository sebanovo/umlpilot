package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.List;
import java.util.Map;

/**
 * Resultado de una ejecución del asistente con metadatos del proveedor que respondió.
 */
public record AiChatResult(
        String reply,
        List<Map<String, Object>> actions,
        String provider,
        String model
) {
}
