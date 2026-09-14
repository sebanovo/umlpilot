package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class OpenRouterProvider implements LlmProvider {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://openrouter.ai/api/v1")
            .build();

    @Override
    public String id() {
        return "openrouter";
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
                    .header("HTTP-Referer", "http://localhost")
                    .header("X-Title", "UMLPilot")
                    .body(Map.of("model", model, "messages", payload))
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content");

            if (content == null || content.isMissingNode() || content.isNull() || content.asText().isBlank()) {
                throw new LlmException("openrouter devolvió una respuesta vacía para el modelo " + model, false);
            }
            return content.asText();
        } catch (RestClientResponseException e) {
            throw LlmException.fromHttp("openrouter", e);
        }
    }
}
