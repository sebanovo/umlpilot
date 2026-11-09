package com.umlpilot.umlpilot_api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del asistente de chat con function calling.
 */
@Component
@ConfigurationProperties(prefix = "app.ai.chat")
public class AiChatProperties {

    /** URL base OpenAI-compatible de OpenRouter. */
    private String openrouterBaseUrl = "https://openrouter.ai/api/v1";

    /** URL base OpenAI-compatible de Google Gemini. */
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";

    /** URL base OpenAI-compatible de DeepSeek. */
    private String deepseekBaseUrl = "https://api.deepseek.com";

    /** Máximo de llamadas a tools dentro de un turno (cota del bucle de tool calling). */
    private int maxToolCalls = 12;

    /** Timeout en segundos para cada llamada al proveedor. */
    private long timeoutSeconds = 60;

    public String getOpenrouterBaseUrl() { return openrouterBaseUrl; }
    public void setOpenrouterBaseUrl(String openrouterBaseUrl) { this.openrouterBaseUrl = openrouterBaseUrl; }

    public String getGeminiBaseUrl() { return geminiBaseUrl; }
    public void setGeminiBaseUrl(String geminiBaseUrl) { this.geminiBaseUrl = geminiBaseUrl; }

    public String getDeepSeekBaseUrl() { return deepseekBaseUrl; }
    public void setDeepSeekBaseUrl(String deepseekBaseUrl) { this.deepseekBaseUrl = deepseekBaseUrl; }

    public int getMaxToolCalls() { return maxToolCalls; }
    public void setMaxToolCalls(int maxToolCalls) { this.maxToolCalls = maxToolCalls; }

    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
}
