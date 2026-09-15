package com.umlpilot.umlpilot_api.infrastructure.ai;

import java.util.Locale;

/**
 * Detecta si un error de un proveedor LLM está relacionado con cuota/créditos (para
 * aplicar un cooldown largo y rotar de proveedor) o con un fallo transitorio.
 */
public final class QuotaErrors {

    private QuotaErrors() {
    }

    public static boolean isQuotaRelated(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("insufficient credit")
                        || normalized.contains("insufficient_quota")
                        || normalized.contains("no credits")
                        || normalized.contains("credit")
                        || normalized.contains("quota")
                        || normalized.contains("rate limit")
                        || normalized.contains("too many requests")
                        || normalized.contains("402")
                        || normalized.contains("429")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
