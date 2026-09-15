package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del asistente de chat con function calling.
 */
@Component
@ConfigurationProperties(prefix = "app.ai.chat")
public class AiChatProperties {

    /** URL base OpenAI-compatible (por defecto OpenRouter). */
    private String baseUrl = "https://openrouter.ai/api/v1";

    /** URL base OpenAI-compatible de Google Gemini. */
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";

    /**
     * Proveedor a usar: {@code openrouter} o {@code gemini}. Si está vacío se toma el
     * primero configurado de {@code app.ai.provider-order}.
     */
    private String provider = "";

    /** Máximo de llamadas a tools dentro de un turno (cota del bucle de tool calling). */
    private int maxToolCalls = 12;

    /** Timeout en segundos para cada llamada al proveedor. */
    private long timeoutSeconds = 60;

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }

    public String getGeminiBaseUrl() { return geminiBaseUrl; }
    public void setGeminiBaseUrl(String geminiBaseUrl) { this.geminiBaseUrl = geminiBaseUrl; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public int getMaxToolCalls() { return maxToolCalls; }
    public void setMaxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; }

    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
