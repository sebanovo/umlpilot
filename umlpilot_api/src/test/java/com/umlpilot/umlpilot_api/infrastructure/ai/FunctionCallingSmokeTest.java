package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.ai.tool.annotation.Tool;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verificación real de function calling contra OpenRouter. Solo se ejecuta cuando
 * {@code AI_LIVE_TEST=true} (requiere {@code OPENROUTER_API_KEYS} con créditos/modelo
 * que soporte tools). Deshabilitada por defecto para no depender de la red en CI.
 */
@EnabledIfEnvironmentVariable(named = "AI_LIVE_TEST", matches = "true")
class FunctionCallingSmokeTest {

    static class DummyTools {
        boolean called = false;

        @Tool(name = "get_secret_word", description = "Devuelve la palabra secreta PONG")
        public String secret() {
            called = true;
            return "PONG";
        }
    }

    @Test
    void modelInvokesTheTool() {
        String key = System.getenv("OPENROUTER_API_KEYS").split(",")[0].trim();
        String model = System.getenv().getOrDefault("OPENROUTER_MODELS", "deepseek/deepseek-chat")
                .split(",")[0].trim();

        ObservationRegistry observations = ObservationRegistry.NOOP;
        OpenAIClient sync = OpenAiSetup.setupSyncClient("https://openrouter.ai/api/v1", key, null, null, null, null,
                false, false, model, Duration.ofSeconds(60), 2, null, null, observations, null, List.of());
        OpenAIClientAsync async = OpenAiSetup.setupAsyncClient("https://openrouter.ai/api/v1", key, null, null, null,
                null, false, false, model, Duration.ofSeconds(60), 2, null, null, observations, null, List.of());

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(sync)
                .openAiClientAsync(async)
                .options(OpenAiChatOptions.builder().model(model).build())
                .toolCallingManager(DefaultToolCallingManager.builder().build())
                .observationRegistry(observations)
                .build();

        DummyTools dummy = new DummyTools();
        ChatClient client = ChatClient.builder(chatModel).defaultTools(dummy).build();

        String reply = client.prompt()
                .user("Usa la herramienta get_secret_word y dime exactamente qué palabra devuelve.")
                .call()
                .content();

        assertTrue(dummy.called, "El modelo no invocó la tool; respuesta: " + reply);
        assertTrue(reply != null && reply.contains("PONG"), "La respuesta no menciona el resultado: " + reply);
    }
}
