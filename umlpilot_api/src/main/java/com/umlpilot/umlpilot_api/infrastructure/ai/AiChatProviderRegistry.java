package com.umlpilot.umlpilot_api.infrastructure.ai;

import org.springframework.ai.openai.OpenAiChatModel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registro de proveedores/modelos de IA disponibles para el asistente, con rotación y
 * enfriamiento (cooldown) al estilo de {@code AiOrchestrator}. Cada entrada es un par
 * (proveedor, modelo): si un modelo falla se enfría solo ese modelo; si el error es de
 * cuota/créditos se enfría todo el proveedor y se pasa al siguiente.
 */
public class AiChatProviderRegistry {

    private final List<Entry> entries;
    private final Map<String, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final AtomicInteger cursor = new AtomicInteger(0);

    public AiChatProviderRegistry(List<Entry> entries) {
        this.entries = List.copyOf(entries);
    }

    public List<Entry> entries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Índice de la entrada por la que empezar (la última que funcionó). */
    public int cursor() {
        if (entries.isEmpty()) return 0;
        return Math.floorMod(cursor.get(), entries.size());
    }

    public void moveCursorTo(int index) {
        if (!entries.isEmpty()) cursor.set(Math.floorMod(index, entries.size()));
    }

    public boolean isCoolingDown(String cooldownKey) {
        Long until = cooldownUntil.get(cooldownKey);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            cooldownUntil.remove(cooldownKey);
            return false;
        }
        return true;
    }

    /** Enfría un único modelo. */
    public void coolDownEntry(String cooldownKey, int seconds) {
        cooldownUntil.put(cooldownKey, System.currentTimeMillis() + Math.max(1, seconds) * 1000L);
    }

    /** Enfría todos los modelos de un proveedor (p. ej. cuando se agotan sus créditos). */
    public void coolDownProvider(String providerId, int seconds) {
        long until = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        for (Entry entry : entries) {
            if (entry.id().equals(providerId)) {
                cooldownUntil.put(entry.cooldownKey(), until);
            }
        }
    }

    public long remainingCooldownSeconds() {
        long now = System.currentTimeMillis();
        long max = 0;
        for (Entry entry : entries) {
            Long until = cooldownUntil.get(entry.cooldownKey());
            if (until != null && until > now) {
                max = Math.max(max, (until - now) / 1000);
            }
        }
        return max;
    }

    public record Entry(String id, String modelName, OpenAiChatModel model,
                        int quotaCooldownSeconds, int errorCooldownSeconds) {
        public String cooldownKey() {
            return id + ":" + modelName;
        }
    }
}
