package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.List;
import java.util.Map;

/**
 * Resultado de un turno del asistente: texto de respuesta y acciones ejecutadas.
 */
public record AiChatOutcome(String reply, List<Map<String, Object>> actions) {
}
