package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.Locale;

/**
 * Clasifica errores de un proveedor LLM para decidir el enfriamiento y la rotación.
 */
public final class QuotaErrors {

    private QuotaErrors() {
    }

    /**
     * Errores de cuota o límite de uso (pueden ser por combinación key+modelo). Se aplica
     * el cooldown largo de cuota.
     */
    public static boolean isQuotaRelated(Throwable error) {
        return messageContains(error,
                "insufficient credit", "insufficient_quota", "no credits",
                "quota", "rate limit", "too many requests", "402", "429");
    }

    /**
     * Errores de créditos agotados a nivel de cuenta: afectan a todas las combinaciones
     * del proveedor (p. ej. 402 de OpenRouter).
     */
    public static boolean isProviderWideQuota(Throwable error) {
        return messageContains(error, "insufficient credit", "insufficient_quota", "no credits", "402");
    }

    private static boolean messageContains(Throwable error, String... needles) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                for (String needle : needles) {
                    if (normalized.contains(needle)) return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
