package com.umlpilot.umlpilot_api.infrastructure.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatProviderInfo;
import com.umlpilot.umlpilot_api.infrastructure.ai.AiChatProviderRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallLimitBehavior;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Construye un modelo de chat OpenAI-compatible por cada combinación (proveedor, modelo)
 * configurada —OpenRouter y/o Google Gemini (endpoint OpenAI-compatible)— y los registra
 * en {@link AiChatProviderRegistry}. El asistente rota entre ellos cuando un modelo no
 * está disponible o se agotan los créditos de un proveedor. La autoconfiguración de
 * OpenAI está deshabilitada en {@code application.yml} para no requerir una api-key global.
 */
@Configuration
public class AiChatConfiguration {

    @Bean
    public AiChatProviderRegistry aiChatProviderRegistry(AiProperties aiProperties, AiChatProperties chatProperties) {
        List<AiChatProviderRegistry.Entry> entries = new ArrayList<>();

        for (String providerId : providerOrder(aiProperties, chatProperties)) {
            AiProperties.Provider config = aiProperties.providerConfig(providerId);
            if (config == null) continue;

            String apiKey = firstNonBlank(config.getApiKeys());
            List<String> models = nonBlank(config.getModels());
            if (apiKey == null || models.isEmpty()) continue;

            String baseUrl = "gemini".equalsIgnoreCase(providerId)
                    ? chatProperties.getGeminiBaseUrl()
                    : chatProperties.getBaseUrl();

            for (String model : models) {
                entries.add(new AiChatProviderRegistry.Entry(providerId, model,
                        buildModel(baseUrl, apiKey, model, chatProperties),
                        config.getQuotaCooldownSeconds(), config.getErrorCooldownSeconds()));
            }
        }

        return new AiChatProviderRegistry(entries);
    }

    /** Proveedor/modelo principal (el primero de la rotación) para mensajes de estado/error. */
    @Bean
    public AiChatProviderInfo aiChatProviderInfo(AiChatProviderRegistry registry) {
        if (registry.entries().isEmpty()) {
            return new AiChatProviderInfo(null, null, false);
        }
        AiChatProviderRegistry.Entry first = registry.entries().get(0);
        return new AiChatProviderInfo(first.id(), first.modelName(), true);
    }

    private static OpenAiChatModel buildModel(String baseUrl, String apiKey, String model,
                                              AiChatProperties chatProperties) {
        Duration timeout = Duration.ofSeconds(chatProperties.getTimeoutSeconds());
        ObservationRegistry observations = ObservationRegistry.NOOP;

        OpenAIClient sync = OpenAiSetup.setupSyncClient(baseUrl, apiKey, null, null, null, null,
                false, false, model, timeout, 2, null, null, observations, null, List.of());
        OpenAIClientAsync async = OpenAiSetup.setupAsyncClient(baseUrl, apiKey, null, null, null, null,
                false, false, model, timeout, 2, null, null, observations, null, List.of());

        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
                .maxTotalToolCalls(chatProperties.getMaxToolCalls())
                .onLimitExceeded(ToolCallLimitBehavior.RETURN_ERROR_RESPONSE)
                .build();

        return OpenAiChatModel.builder()
                .openAiClient(sync)
                .openAiClientAsync(async)
                .options(OpenAiChatOptions.builder().model(model).build())
                .toolCallingManager(toolCallingManager)
                .observationRegistry(observations)
                .build();
    }

    /**
     * Orden de proveedores para la rotación: primero {@code app.ai.chat.provider} (si está
     * definido) y luego el resto de {@code app.ai.provider-order}, sin duplicados.
     */
    static List<String> providerOrder(AiProperties aiProperties, AiChatProperties chatProperties) {
        LinkedHashSet<String> order = new LinkedHashSet<>();

        String chosen = chatProperties.getProvider();
        if (chosen != null && !chosen.isBlank()) {
            order.add(chosen.trim().toLowerCase());
        }

        List<String> configuredOrder = aiProperties.getProviderOrder();
        if (configuredOrder != null) {
            for (String candidate : configuredOrder) {
                if (candidate != null && !candidate.isBlank()) {
                    order.add(candidate.trim().toLowerCase());
                }
            }
        }

        if (order.isEmpty()) {
            order.add("openrouter");
            order.add("gemini");
        }
        return new ArrayList<>(order);
    }

    private static String firstNonBlank(List<String> values) {
        if (values == null) return null;
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    private static List<String> nonBlank(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }
}
