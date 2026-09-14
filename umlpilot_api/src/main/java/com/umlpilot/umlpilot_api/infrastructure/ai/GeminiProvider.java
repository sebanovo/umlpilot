package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiProvider implements LlmProvider {

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com/v1beta")
            .build();

    @Override
    public String id() {
        return "gemini";
    }

    @Override
    public String complete(String apiKey, String model, List<LlmMessage> messages) {
        List<Map<String, Object>> contents = new ArrayList<>();
        String systemInstruction = null;

        for (LlmMessage message : messages) {
            if ("system".equalsIgnoreCase(message.role())) {
                systemInstruction = message.content();
                continue;
            }
            String role = "assistant".equalsIgnoreCase(message.role()) ? "model" : "user";
            contents.add(Map.of("role", role, "parts", List.of(Map.of("text", message.content()))));
        }

        Map<String, Object> body = new HashMap<>();
        body.put("contents", contents);
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemInstruction))));
        }

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/models/{model}:generateContent").queryParam("key", apiKey).build(model))
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode text = response == null
                    ? null
                    : response.path("candidates").path(0).path("content").path("parts").path(0).path("text");

            if (text == null || text.isMissingNode() || text.isNull() || text.asText().isBlank()) {
                throw new LlmException("gemini devolvió una respuesta vacía para el modelo " + model, false);
            }
            return text.asText();
        } catch (RestClientResponseException e) {
            throw LlmException.fromHttp("gemini", e);
        }
    }
}
