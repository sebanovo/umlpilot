package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Proveedor DeepSeek (API OpenAI-compatible en {@code /chat/completions}).
 * Se usa tanto por el chat con function calling (vía {@code AiChatConfiguration})
 * como por el orquestador legacy ({@link AiOrchestrator}).
 */
@Component
public class DeepSeekProvider implements LlmProvider {

    private final RestClient restClient;

    public DeepSeekProvider(
            @Value("${app.ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String id() {
        return "deepseek";
    }

    @Override
    public String complete(String apiKey, String model, List<LlmMessage> messages) {
        List<Map<String, String>> payload = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content()))
                .toList();

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(Map.of("model", model, "messages", payload, "stream", false))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content");

            if (content == null || content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new LlmException("deepseek devolvió una respuesta vacía para el modelo " + model, false);
            }
            return content.asText();
        } catch (RestClientResponseException e) {
            throw LlmException.fromHttp("deepseek", e);
        }
    }
}
