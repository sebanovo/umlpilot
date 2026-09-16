package com.umlpilot.umlpilot_api.infrastructure.ai;

import com.umlpilot.umlpilot_api.infrastructure.config.GroqProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Transcripción de audio a texto usando Groq (Whisper) por su API OpenAI-compatible.
 */
@Service
public class GroqTranscriptionService {

    private final GroqProperties properties;
    private final RestClient restClient = RestClient.builder().build();

    public GroqTranscriptionService(GroqProperties properties) {
        this.properties = properties;
    }

    public String transcribe(byte[] audio, String filename) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new LlmException("La transcripción por voz no está configurada (falta GROQ_API_KEY)", false);
        }
        if (audio == null || audio.length == 0) {
            throw new LlmException("El audio está vacío", false);
        }

        ByteArrayResource resource = new ByteArrayResource(audio) {
            @Override
            public String getFilename() {
                return (filename == null || filename.isBlank()) ? "audio.webm" : filename;
            }
        };

        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", resource);
        parts.add("model", properties.getModel());
        if (properties.getLanguage() != null && !properties.getLanguage().isBlank()) {
            parts.add("language", properties.getLanguage());
        }

        try {
            Map<?, ?> body = restClient.post()
                    .uri(properties.getBaseUrl() + "/audio/transcriptions")
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(Map.class);

            Object text = body == null ? null : body.get("text");
            if (text == null) {
                throw new LlmException("Respuesta de transcripción sin texto", false);
            }
            return text.toString().trim();
        } catch (RestClientResponseException e) {
            throw LlmException.fromHttp("groq", e);
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Error al transcribir el audio: " + e.getMessage(), false);
        }
    }
}
