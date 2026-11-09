package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.infrastructure.config.AiProperties;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orquesta las llamadas a los proveedores de IA con rotación automática.
 *
 * Estrategia:
 *  - Se recorren los proveedores en el orden configurado (app.ai.provider-order).
 *  - Dentro de cada proveedor se prueban todas las combinaciones (api-key x modelo).
 *  - Si una key/modelo se agota (cuota/tokens) o falla, se marca en "cooldown" y se pasa al siguiente.
 *  - Si un proveedor completo se agota, el cursor avanza al siguiente proveedor (y viceversa).
 */
@Service
public class AiOrchestrator {

    private final AiProperties properties;
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();
    private final Map<String, Long> cooldownUntil = new ConcurrentHashMap<>();
    private final AtomicInteger providerCursor = new AtomicInteger(0);

    public AiOrchestrator(AiProperties properties, List<LlmProvider> providerBeans) {
        this.properties = properties;
        for (LlmProvider provider : providerBeans) {
            this.providers.put(provider.id(), provider);
        }
    }

    public LlmResponse complete(List<LlmMessage> messages) {
        List<String> order = providerOrder();
        int size = order.size();
        int start = Math.floorMod(providerCursor.get(), size);
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            String providerId = order.get((start + i) % size);
            LlmProvider provider = providers.get(providerId);
            AiProperties.Provider config = properties.providerConfig(providerId);
            if (provider == null || config == null) continue;

            LlmResponse response = tryProvider(provider, config, messages, errors);
            if (response != null) {
                providerCursor.set(order.indexOf(providerId));
                return response;
            }
            // Proveedor agotado -> al siguiente proveedor
            providerCursor.incrementAndGet();
        }

        throw new LlmException(
                "Todos los proveedores/modelos/API keys están agotados o fallaron: " + String.join(" | ", errors),
                true
        );
    }

    private LlmResponse tryProvider(
            LlmProvider provider,
            AiProperties.Provider config,
            List<LlmMessage> messages,
            List<String> errors
    ) {
        List<String> keys = nonBlank(config.getApiKeys());
        List<String> models = nonBlank(config.getModels());

        if (keys.isEmpty() || models.isEmpty()) {
            errors.add(provider.id() + ": sin api-keys o models configurados");
            return null;
        }

        for (String key : keys) {
            for (String model : models) {
                String cooldownKey = provider.id() + ":" + Integer.toHexString(key.hashCode()) + ":" + model;
                if (isCoolingDown(cooldownKey)) continue;

                try {
                    String content = provider.complete(key, model, messages);
                    return new LlmResponse(provider.id(), model, content);
                } catch (LlmException e) {
                    errors.add(provider.id() + "[" + mask(key) + "/" + model + "]: " + e.getMessage());
                    long seconds = e.isQuotaRelated()
                            ? config.getQuotaCooldownSeconds()
                            : config.getErrorCooldownSeconds();
                    cooldown(cooldownKey, seconds);
                } catch (Exception e) {
                    errors.add(provider.id() + "[" + mask(key) + "/" + model + "]: " + e.getMessage());
                    cooldown(cooldownKey, config.getErrorCooldownSeconds());
                }
            }
        }
        return null;
    }

    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String providerId : providerOrder()) {
            AiProperties.Provider config = properties.providerConfig(providerId);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("models", config == null ? List.of() : nonBlank(config.getModels()));
            info.put("apiKeys", config == null ? 0 : nonBlank(config.getApiKeys()).size());
            result.put(providerId, info);
        }

        long now = System.currentTimeMillis();
        Map<String, Long> active = new LinkedHashMap<>();
        cooldownUntil.forEach((key, until) -> {
            if (until > now) active.put(key, (until - now) / 1000);
        });
        result.put("cooldownSeconds", active);
        return result;
    }

    private List<String> providerOrder() {
        List<String> order = properties.getProviderOrder();
        if (order == null || order.isEmpty()) {
            return List.of("openrouter", "gemini", "deepseek");
        }
        return order;
    }

    private boolean isCoolingDown(String key) {
        Long until = cooldownUntil.get(key);
        if (until == null) return false;
        if (until <= System.currentTimeMillis()) {
            cooldownUntil.remove(key);
            return false;
        }
        return true;
    }

    private void cooldown(String key, long seconds) {
        cooldownUntil.put(key, System.currentTimeMillis() + Math.max(1, seconds) * 1000L);
    }

    private static List<String> nonBlank(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
    }

    private static String mask(String key) {
        if (key == null || key.length() <= 8) return "***";
        return key.substring(0, 4) + "…" + key.substring(key.length() - 4);
    }
}
