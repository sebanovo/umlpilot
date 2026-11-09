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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Construye un modelo de chat OpenAI-compatible por cada combinación (proveedor, modelo)
 * configurada —DeepSeek, OpenRouter y/o Google Gemini (endpoints OpenAI-compatible)— y los
 * registra en {@link AiChatProviderRegistry}. Hay dos registros: el de chat (texto) y el de visión
 * (imagen + tools, para construir el diagrama desde una imagen). El asistente rota entre
 * entradas cuando un modelo no está disponible o se agotan los créditos de un proveedor.
 * La autoconfiguración de OpenAI está deshabilitada en {@code application.yml} para no
 * requerir una api-key global.
 */
@Configuration
public class AiChatConfiguration {

    /** Proveedores para el chat de texto. */
    @Bean
    public AiChatProviderRegistry aiChatProviderRegistry(AiProperties aiProperties, AiChatProperties chatProperties) {
        return buildRegistry(aiProperties, chatProperties, false);
    }

    /** Proveedores con visión (imagen + tools) para construir desde una imagen. */
    @Bean
    public AiChatProviderRegistry aiChatVisionRegistry(AiProperties aiProperties, AiChatProperties chatProperties) {
        return buildRegistry(aiProperties, chatProperties, true);
    }

    /** Proveedor/modelo principal del chat (el primero de la rotación) para mensajes de estado. */
    @Bean
    public AiChatProviderInfo aiChatProviderInfo(
            @Qualifier("aiChatProviderRegistry") AiChatProviderRegistry registry) {
        if (registry.entries().isEmpty()) {
            return new AiChatProviderInfo(null, null, false);
        }
        AiChatProviderRegistry.Entry first = registry.entries().get(0);
        return new AiChatProviderInfo(first.id(), first.modelName(), true);
    }

    private AiChatProviderRegistry buildRegistry(AiProperties aiProperties, AiChatProperties chatProperties,
                                                 boolean vision) {
        List<AiChatProviderRegistry.Entry> entries = new ArrayList<>();

        for (String providerId : providerOrder(aiProperties)) {
            AiProperties.Provider config = aiProperties.providerConfig(providerId);
            if (config == null) continue;

            List<String> keys = nonBlank(config.getApiKeys());
            List<String> models = vision ? visionModelsFor(providerId, config) : nonBlank(config.getModels());
            if (keys.isEmpty() || models.isEmpty()) continue;

            String baseUrl = switch (providerId.toLowerCase()) {
                case "gemini" -> chatProperties.getGeminiBaseUrl();
                case "deepseek" -> chatProperties.getDeepSeekBaseUrl();
                default -> chatProperties.getOpenrouterBaseUrl();
            };

            for (String key : keys) {
                String keyRef = Integer.toHexString(key.hashCode());
                for (String model : models) {
                    entries.add(new AiChatProviderRegistry.Entry(providerId, model, keyRef,
                            buildModel(baseUrl, key, model, chatProperties),
                            config.getQuotaCooldownSeconds(), config.getErrorCooldownSeconds()));
                }
            }
        }

        return new AiChatProviderRegistry(entries);
    }

    /**
     * Modelos con visión de un proveedor: su lista explícita {@code vision-models} si la
     * hay; si no, para Gemini se usan los modelos normales (todos aceptan imagen) y para
     * OpenRouter/DeepSeek ninguno (solo los declarados como visión aceptan imagen).
     */
    private static List<String> visionModelsFor(String providerId, AiProperties.Provider config) {
        List<String> explicit = nonBlank(config.getVisionModels());
        if (!explicit.isEmpty()) return explicit;
        if ("gemini".equalsIgnoreCase(providerId)) return nonBlank(config.getModels());
        return List.of();
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
     * Orden de proveedores para la rotación: {@code app.ai.provider-order}
     * ({@code AI_PROVIDER_ORDER}), sin duplicados.
     */
    static List<String> providerOrder(AiProperties aiProperties) {
        LinkedHashSet<String> order = new LinkedHashSet<>();

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
            order.add("deepseek");
        }
        return new ArrayList<>(order);
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
