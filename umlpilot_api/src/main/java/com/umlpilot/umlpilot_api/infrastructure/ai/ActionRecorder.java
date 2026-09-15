package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recoge las acciones que ejecuta el asistente durante una conversación para
 * devolverlas al cliente. Vive en el {@code ToolContext} de una petición, por lo que
 * cada conversación tiene su propia instancia.
 */
public class ActionRecorder {

    private final List<Map<String, Object>> actions = new ArrayList<>();

    public void record(String action, String entity, String entityId, String result) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("action", action);
        entry.put("entity", entity);
        entry.put("id", entityId);
        entry.put("result", result);
        actions.add(entry);
    }

    public List<Map<String, Object>> actions() {
        return List.copyOf(actions);
    }

    public boolean hasMutations() {
        return actions.stream().anyMatch(a -> !"read".equals(a.get("action")));
    }
}
